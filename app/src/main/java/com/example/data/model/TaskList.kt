package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class TaskList(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val color: String = "#6750A4",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val memberUids: List<String> = emptyList(),
    val memberEmails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
