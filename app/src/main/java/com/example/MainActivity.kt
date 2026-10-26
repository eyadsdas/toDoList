package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.TaskRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.splash.SplashScreen
import com.example.ui.tasks.TaskDashboardScreen
import com.example.ui.tasks.TaskViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showSplash by remember { mutableStateOf(true) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    // Attempt silent auto-sign-in on startup
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    if (showSplash) {
        SplashScreen(
            onDismiss = { showSplash = false }
        )
    } else {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = {
                    // AuthStateListener will automatically update currentUser
                }
            )
        } else {
            val taskViewModel: TaskViewModel = viewModel(
                key = user.uid,
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        val app = context.applicationContext as Application
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        try {
                            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                                .build()
                        } catch (_: Exception) {}
                        return TaskViewModel(
                            application = app,
                            repository = TaskRepository(db),
                            currentUserId = user.uid
                        ) as T
                    }
                }
            )

            TaskDashboardScreen(
                viewModel = taskViewModel,
                onSignOut = {
                    // User signed out, will reactively re-render AuthScreen
                }
            )
        }
    }
}
