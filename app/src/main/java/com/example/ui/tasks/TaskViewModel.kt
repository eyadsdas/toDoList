package com.example.ui.tasks

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ActivityItem
import com.example.data.model.Task
import com.example.data.model.TaskList
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    application: Application,
    private val repository: TaskRepository,
    val currentUserId: String
) : AndroidViewModel(application) {

    private val _isOnline = MutableStateFlow(checkInitialConnectivity(application))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _selectedList = MutableStateFlow<TaskList?>(null)
    val selectedList: StateFlow<TaskList?> = _selectedList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow<Boolean?>(null) // null = all, false = pending, true = completed
    val filterStatus: StateFlow<Boolean?> = _filterStatus.asStateFlow()

    private val _filterPriority = MutableStateFlow<String?>(null) // null = all, "high", "medium", "low"
    val filterPriority: StateFlow<String?> = _filterPriority.asStateFlow()

    private var previousActivityIds = mutableSetOf<String>()
    private val _latestActivityNotification = MutableStateFlow<ActivityItem?>(null)
    val latestActivityNotification: StateFlow<ActivityItem?> = _latestActivityNotification.asStateFlow()

    // 1. Task Lists Stream
    val taskLists: StateFlow<UiState<List<TaskList>>> = repository.observeTaskLists(currentUserId)
        .map<List<TaskList>, UiState<List<TaskList>>> { lists ->
            if (_selectedList.value == null && lists.isNotEmpty()) {
                _selectedList.value = lists.first()
            } else if (_selectedList.value != null && lists.none { it.id == _selectedList.value?.id }) {
                _selectedList.value = lists.firstOrNull()
            }
            UiState.Success(lists)
        }
        .catch { error ->
            Log.e(TAG, "Failed observing task lists", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to observe task lists"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 2. Tasks Stream for Selected List
    val tasks: StateFlow<UiState<List<Task>>> = _selectedList
        .flatMapLatest { list ->
            if (list == null) {
                flowOf(emptyList())
            } else {
                repository.observeTasks(list.id, currentUserId)
            }
        }
        .map<List<Task>, UiState<List<Task>>> { list ->
            UiState.Success(list)
        }
        .catch { error ->
            Log.e(TAG, "Failed observing tasks", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to observe tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    // 3. Activity / Notification Stream
    val activities: StateFlow<List<ActivityItem>> = repository.observeActivities(currentUserId)
        .map { list ->
            // Detect real-time updates from other collaborators
            if (previousActivityIds.isNotEmpty()) {
                val newIncoming = list.firstOrNull { it.id !in previousActivityIds && it.actorId != currentUserId }
                if (newIncoming != null) {
                    _latestActivityNotification.value = newIncoming
                }
            }
            previousActivityIds = list.map { it.id }.toMutableSet()
            list
        }
        .catch { error ->
            Log.w(TAG, "Activity stream error", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        registerNetworkMonitoring(application)
    }

    private fun checkInitialConnectivity(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkMonitoring(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }
                override fun onLost(network: Network) {
                    _isOnline.value = false
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "NetworkCallback registration failed", e)
        }
    }

    fun selectList(list: TaskList) {
        _selectedList.value = list
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: Boolean?) {
        _filterStatus.value = status
    }

    fun setFilterPriority(priority: String?) {
        _filterPriority.value = priority
    }

    fun dismissNotification() {
        _latestActivityNotification.value = null
    }

    fun createTaskList(title: String, description: String = "", color: String = "#6750A4") {
        viewModelScope.launch {
            val result = repository.createTaskList(title, description, color)
            result.onSuccess { listId ->
                _userMessage.emit("Created list \"$title\"")
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to create list")
            }
        }
    }

    fun createTask(
        title: String,
        description: String = "",
        priority: String = "medium",
        dueDate: String = "",
        assignedTo: String = ""
    ) {
        val list = _selectedList.value ?: return
        viewModelScope.launch {
            val result = repository.createTask(
                list = list,
                title = title,
                description = description,
                priority = priority,
                dueDate = dueDate,
                assignedTo = assignedTo
            )
            result.onSuccess {
                _userMessage.emit("Added task \"$title\"")
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to add task")
            }
        }
    }

    fun toggleTask(task: Task) {
        val list = _selectedList.value ?: return
        viewModelScope.launch {
            val result = repository.toggleTaskCompleted(task, list.title)
            result.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to update task")
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val result = repository.updateTask(task)
            result.onSuccess {
                _userMessage.emit("Task updated")
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(task: Task) {
        val list = _selectedList.value ?: return
        viewModelScope.launch {
            val result = repository.deleteTask(task, list.title)
            result.onSuccess {
                _userMessage.emit("Task deleted")
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to delete task")
            }
        }
    }

    fun deleteList(list: TaskList) {
        viewModelScope.launch {
            val result = repository.deleteTaskList(list.id)
            result.onSuccess {
                _userMessage.emit("List deleted")
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to delete list")
            }
        }
    }

    fun addCollaborator(collaboratorEmail: String) {
        val list = _selectedList.value ?: return
        val email = collaboratorEmail.trim().lowercase()
        if (email.isBlank() || !email.contains("@")) {
            viewModelScope.launch { _userMessage.emit("Please enter a valid email address") }
            return
        }
        viewModelScope.launch {
            // In shared lists, collaborator uid can be derived or matched
            val result = repository.addCollaborator(list, email, email)
            result.onSuccess {
                _userMessage.emit("Added $email as collaborator!")
            }.onFailure { err ->
                _userMessage.emit(err.localizedMessage ?: "Failed to add collaborator")
            }
        }
    }

    private companion object {
        const val TAG = "TaskVM"
    }
}
