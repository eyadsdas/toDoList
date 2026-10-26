package com.example.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ActivityItem
import com.example.data.model.Task
import com.example.data.model.TaskList
import com.example.ui.auth.performSignOut
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDashboardScreen(
    viewModel: TaskViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { androidx.credentials.CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val taskListsState by viewModel.taskLists.collectAsStateWithLifecycle()
    val selectedList by viewModel.selectedList.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasks.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    val filterPriority by viewModel.filterPriority.collectAsStateWithLifecycle()
    val latestNotification by viewModel.latestActivityNotification.collectAsStateWithLifecycle()

    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showShareListDialog by remember { mutableStateOf(false) }
    var showActivityFeed by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    val activitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // SnackBar listener for user messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Eyad Alshareef",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "TaskFlow",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                title = {
                    // Online / Offline Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isOnline) Color(0xFF10B981) else Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isOnline) "Live Sync" else "Offline Mode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) Color(0xFF10B981) else Color(0xFFD97706)
                            )
                        }
                    }
                },
                actions = {
                    // Notifications / Activity Feed
                    IconButton(onClick = { showActivityFeed = true }) {
                        BadgedBox(
                            badge = {
                                if (activities.isNotEmpty()) {
                                    Badge { Text("${activities.size.coerceAtMost(99)}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Activity Feed",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Profile / Sign-out menu
                    Box {
                        IconButton(onClick = { showProfileMenu = true }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = (Firebase.auth.currentUser?.displayName?.take(1)
                                        ?: Firebase.auth.currentUser?.email?.take(1)
                                        ?: "E").uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = Firebase.auth.currentUser?.displayName ?: "Eyad Alshareef",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = Firebase.auth.currentUser?.email ?: "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Sign Out") },
                                onClick = {
                                    showProfileMenu = false
                                    performSignOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onSignOut,
                                        scope = coroutineScope
                                    )
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedList != null) {
                FloatingActionButton(
                    onClick = { showCreateTaskDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Real-time Collaborator Notification Banner
            AnimatedVisibility(
                visible = latestNotification != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                latestNotification?.let { notif ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${notif.actorName} ${notif.action} \"${notif.taskTitle}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissNotification() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Task Lists Selector Row
            TaskListsRow(
                taskListsState = taskListsState,
                selectedList = selectedList,
                onSelectList = { viewModel.selectList(it) },
                onAddNewList = { showCreateListDialog = true }
            )

            // Current List Header & Actions
            selectedList?.let { currentList ->
                ListHeaderCard(
                    taskList = currentList,
                    onShareClick = { showShareListDialog = true },
                    onDeleteList = { viewModel.deleteList(currentList) },
                    currentUserId = viewModel.currentUserId
                )
            }

            // Filters & Search
            FilterAndSearchRow(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                filterStatus = filterStatus,
                onStatusFilterChange = { viewModel.setFilterStatus(it) },
                filterPriority = filterPriority,
                onPriorityFilterChange = { viewModel.setFilterPriority(it) }
            )

            // Tasks Content
            Box(modifier = Modifier.weight(1f)) {
                when (val state = tasksState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        val filteredTasks = state.data.filter { task ->
                            val matchesQuery = searchQuery.isBlank() ||
                                task.title.contains(searchQuery, ignoreCase = true) ||
                                task.description.contains(searchQuery, ignoreCase = true)
                            val matchesStatus = filterStatus == null || task.completed == filterStatus
                            val matchesPriority = filterPriority == null || task.priority.equals(filterPriority, ignoreCase = true)
                            matchesQuery && matchesStatus && matchesPriority
                        }

                        if (filteredTasks.isEmpty()) {
                            EmptyTasksPlaceholder(
                                hasList = selectedList != null,
                                onAddTask = { showCreateTaskDialog = true },
                                onCreateList = { showCreateListDialog = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredTasks, key = { it.id }) { task ->
                                    TaskItemCard(
                                        task = task,
                                        onToggleCompleted = { viewModel.toggleTask(task) },
                                        onEdit = { editingTask = task },
                                        onDelete = { viewModel.deleteTask(task) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateTaskDialog = false },
            onConfirm = { title, desc, priority, dueDate, assignee ->
                showCreateTaskDialog = false
                viewModel.createTask(title, desc, priority, dueDate, assignee)
            }
        )
    }

    if (showCreateListDialog) {
        CreateListDialog(
            onDismiss = { showCreateListDialog = false },
            onConfirm = { title, desc, colorHex ->
                showCreateListDialog = false
                viewModel.createTaskList(title, desc, colorHex)
            }
        )
    }

    selectedList?.let { currentList ->
        if (showShareListDialog) {
            ShareListDialog(
                taskList = currentList,
                onDismiss = { showShareListDialog = false },
                onAddCollaborator = { email ->
                    viewModel.addCollaborator(email)
                }
            )
        }
    }

    if (showActivityFeed) {
        ActivityFeedSheet(
            sheetState = activitySheetState,
            activities = activities,
            onDismiss = { showActivityFeed = false }
        )
    }
}

@Composable
private fun TaskListsRow(
    taskListsState: UiState<List<TaskList>>,
    selectedList: TaskList?,
    onSelectList: (TaskList) -> Unit,
    onAddNewList: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (taskListsState) {
            is UiState.Success -> {
                taskListsState.data.forEach { list ->
                    val isSelected = selectedList?.id == list.id
                    val listColor = try {
                        Color(android.graphics.Color.parseColor(list.color))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectList(list) }
                            .testTag("list_tab_${list.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(listColor)
                            )
                            Text(
                                text = list.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (list.memberUids.size > 1) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "Collaborative List",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        // Add new list button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onAddNewList() }
                .testTag("add_list_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New List",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "New List",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ListHeaderCard(
    taskList: TaskList,
    onShareClick: () -> Unit,
    onDeleteList: () -> Unit,
    currentUserId: String
) {
    var showMenu by remember { mutableStateOf(false) }
    val isOwner = taskList.ownerId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = taskList.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (taskList.description.isNotBlank()) {
                    Text(
                        text = taskList.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Share / Invite button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onShareClick() }
                        .testTag("share_list_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${taskList.memberEmails.size.coerceAtLeast(1)} members",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete List", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteList()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterAndSearchRow(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: Boolean?,
    onStatusFilterChange: (Boolean?) -> Unit,
    filterPriority: String?,
    onPriorityFilterChange: (String?) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search tasks...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterStatus == null && filterPriority == null,
                onClick = {
                    onStatusFilterChange(null)
                    onPriorityFilterChange(null)
                },
                label = { Text("All") }
            )

            FilterChip(
                selected = filterStatus == false,
                onClick = {
                    onStatusFilterChange(if (filterStatus == false) null else false)
                },
                label = { Text("Pending") }
            )

            FilterChip(
                selected = filterStatus == true,
                onClick = {
                    onStatusFilterChange(if (filterStatus == true) null else true)
                },
                label = { Text("Completed") }
            )

            FilterChip(
                selected = filterPriority == "high",
                onClick = {
                    onPriorityFilterChange(if (filterPriority == "high") null else "high")
                },
                label = { Text("High Priority") }
            )
        }
    }
}

@Composable
private fun TaskItemCard(
    task: Task,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleCompleted() },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Pill
                    val (pColor, pLabel) = when (task.priority.lowercase()) {
                        "high" -> PriorityHigh to "High"
                        "low" -> PriorityLow to "Low"
                        else -> PriorityMedium to "Medium"
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = pColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = pLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = pColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (task.dueDate.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = task.dueDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (task.assignedTo.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = task.assignedTo,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Task Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder(
    hasList: Boolean,
    onAddTask: () -> Unit,
    onCreateList: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Text(
                text = if (hasList) "No tasks found" else "No task lists yet",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasList) "Tap the + button to add your first collaborative task." else "Create a shared list to start syncing across devices.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 18.sp
            )
            if (hasList) {
                TextButton(onClick = onAddTask) {
                    Text("+ Add Task")
                }
            } else {
                TextButton(onClick = onCreateList) {
                    Text("+ Create First List")
                }
            }
        }
    }
}
