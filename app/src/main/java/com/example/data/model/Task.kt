package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val listId: String = "",
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val priority: String = "medium", // "low", "medium", "high"
    val dueDate: String = "",
    val assignedTo: String = "",
    val creatorId: String = "",
    val memberUids: List<String> = emptyList(),
    val lastUpdatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
