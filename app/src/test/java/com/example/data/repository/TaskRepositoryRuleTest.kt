package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.TaskList
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createTaskList_authenticatedUser_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTaskList("Work Projects", "Important items")
    }
    assertTrue("Expected list creation to succeed", result.isSuccess)
    val listId = result.getOrThrow()
    assertTrue(listId.isNotEmpty())
  }

  @Test
  fun observeTaskLists_authenticatedUser_emitsCreatedList() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)

    val listId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTaskList("Sprint Tasks").getOrThrow()
    }

    val emittedLists = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTaskLists(auth.currentUser!!.uid).first { lists ->
        lists.any { it.id == listId }
      }
    }
    assertTrue(emittedLists.any { it.id == listId })
  }

  @Test
  fun observeTaskLists_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = TaskRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeTaskLists("unauth_user").first()
      }
      fail("Expected exception for unauthenticated call")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }
  }

  @Test
  fun collaborativeTask_accessByMember_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = TaskRepository(firestore)

    val list = TaskList(
      id = "shared_test_list",
      title = "Team Board",
      ownerId = aliceUid,
      ownerName = "Alice",
      memberUids = listOf(aliceUid, "bob_test_uid")
    )
    firestore.collection("task_lists").document(list.id).set(list)

    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createTask(list, "Initial Task").getOrThrow()
    }
    assertTrue(taskId.isNotEmpty())

    // Sign in as Bob
    val bobUid = signInTestUser(BOB_EMAIL)
    // Update list to have actual bobUid
    val updatedList = list.copy(memberUids = listOf(aliceUid, bobUid))
    firestore.collection("task_lists").document(list.id).set(updatedList)

    val bobRepo = TaskRepository(firestore)
    val tasks = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observeTasks(list.id, bobUid).first()
    }
    assertNotNull(tasks)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_rules@test.com"
    const val BOB_EMAIL = "bob_rules@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
