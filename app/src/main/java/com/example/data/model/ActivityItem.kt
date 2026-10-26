package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ActivityItem(
    @DocumentId val id: String = "",
    val listId: String = "",
    val listTitle: String = "",
    val memberUids: List<String> = emptyList(),
    val actorId: String = "",
    val actorName: String = "",
    val action: String = "",
    val taskTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
