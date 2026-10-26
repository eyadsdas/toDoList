package com.example.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, priority: String, dueDate: String, assignedTo: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var dueDate by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Task",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showError = false
                    },
                    label = { Text("Task Title *") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Title, contentDescription = null)
                    },
                    isError = showError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )
                if (showError) {
                    Text(
                        text = "Task title is required",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null)
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selector
                Text(
                    text = "Priority Level",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "low" to ("Low" to PriorityLow),
                        "medium" to ("Medium" to PriorityMedium),
                        "high" to ("High" to PriorityHigh)
                    ).forEach { (pKey, pair) ->
                        val (pLabel, pColor) = pair
                        val isSelected = priority == pKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = pKey },
                            label = { Text(pLabel) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(pColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pColor.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (e.g. Tomorrow, Oct 30)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text("Assignee (Name or Email)") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(title, description, priority, dueDate, assignedTo)
                    }
                },
                modifier = Modifier.testTag("confirm_create_task_button")
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
