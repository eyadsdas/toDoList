package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ActivityItem
import com.example.data.model.Task
import com.example.data.model.TaskList
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TaskRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun getUserDisplayName(): String {
        return auth.currentUser?.displayName?.ifBlank { "User" } ?: auth.currentUser?.email?.substringBefore("@") ?: "User"
    }

    // Observe all task lists the user is a member of
    fun observeTaskLists(userId: String): Flow<List<TaskList>> {
        val path = "task_lists"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(TaskList::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // Observe tasks for a specific list
    fun observeTasks(listId: String, userId: String): Flow<List<Task>> {
        val path = "tasks"
        return db.collection(path)
            .whereEqualTo("listId", listId)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedWith(
                    compareBy<Task> { it.completed }
                        .thenByDescending { it.createdAt }
                )
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // Observe recent collaborative activities / notifications
    fun observeActivities(userId: String): Flow<List<ActivityItem>> {
        val path = "activities"
        return db.collection(path)
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ActivityItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // Create a new task list
    suspend fun createTaskList(
        title: String,
        description: String = "",
        color: String = "#6750A4"
    ): Result<String> {
        val uid = requireUserId()
        val docId = UUID.randomUUID().toString()
        val docRef = db.collection("task_lists").document(docId)
        val data = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "color" to color,
            "ownerId" to uid,
            "ownerName" to getUserDisplayName(),
            "ownerEmail" to (auth.currentUser?.email ?: ""),
            "memberUids" to listOf(uid),
            "memberEmails" to listOfNotNull(auth.currentUser?.email),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(data).await()
            logActivity(
                listId = docId,
                listTitle = title,
                memberUids = listOf(uid),
                action = "created list",
                taskTitle = ""
            )
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // Add collaborator to list by user ID or email
    suspend fun addCollaborator(
        list: TaskList,
        collaboratorUid: String,
        collaboratorEmail: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("task_lists").document(list.id)
        val newMemberUids = (list.memberUids + collaboratorUid).distinct()
        val newMemberEmails = (list.memberEmails + collaboratorEmail.trim().lowercase()).distinct()
        return try {
            docRef.update(
                mapOf(
                    "memberUids" to newMemberUids,
                    "memberEmails" to newMemberEmails,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                listId = list.id,
                listTitle = list.title,
                memberUids = newMemberUids,
                action = "added collaborator $collaboratorEmail",
                taskTitle = ""
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // Create a new task within a list
    suspend fun createTask(
        list: TaskList,
        title: String,
        description: String = "",
        priority: String = "medium",
        dueDate: String = "",
        assignedTo: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val taskId = UUID.randomUUID().toString()
        val docRef = db.collection("tasks").document(taskId)
        val data = mapOf(
            "listId" to list.id,
            "title" to title.trim(),
            "description" to description.trim(),
            "completed" to false,
            "priority" to priority,
            "dueDate" to dueDate.trim(),
            "assignedTo" to assignedTo.trim(),
            "creatorId" to uid,
            "memberUids" to list.memberUids,
            "lastUpdatedBy" to getUserDisplayName(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(data).await()
            logActivity(
                listId = list.id,
                listTitle = list.title,
                memberUids = list.memberUids,
                action = "added task",
                taskTitle = title
            )
            Result.success(taskId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // Toggle task completed status
    suspend fun toggleTaskCompleted(task: Task, listTitle: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("tasks").document(task.id)
        val newStatus = !task.completed
        return try {
            docRef.update(
                mapOf(
                    "completed" to newStatus,
                    "lastUpdatedBy" to getUserDisplayName(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                listId = task.listId,
                listTitle = listTitle,
                memberUids = task.memberUids,
                action = if (newStatus) "completed task" else "reopened task",
                taskTitle = task.title
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // Update task details
    suspend fun updateTask(task: Task): Result<Unit> {
        requireUserId()
        val docRef = db.collection("tasks").document(task.id)
        return try {
            docRef.update(
                mapOf(
                    "title" to task.title,
                    "description" to task.description,
                    "priority" to task.priority,
                    "dueDate" to task.dueDate,
                    "assignedTo" to task.assignedTo,
                    "lastUpdatedBy" to getUserDisplayName(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // Delete a task
    suspend fun deleteTask(task: Task, listTitle: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("tasks").document(task.id)
        return try {
            docRef.delete().await()
            logActivity(
                listId = task.listId,
                listTitle = listTitle,
                memberUids = task.memberUids,
                action = "deleted task",
                taskTitle = task.title
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Delete a task list
    suspend fun deleteTaskList(listId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("task_lists").document(listId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private suspend fun logActivity(
        listId: String,
        listTitle: String,
        memberUids: List<String>,
        action: String,
        taskTitle: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val activityId = UUID.randomUUID().toString()
        val docRef = db.collection("activities").document(activityId)
        val data = mapOf(
            "listId" to listId,
            "listTitle" to listTitle,
            "memberUids" to memberUids,
            "actorId" to uid,
            "actorName" to getUserDisplayName(),
            "action" to action,
            "taskTitle" to taskTitle,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (_: Exception) {
            // Non-critical logging
        }
    }
}
