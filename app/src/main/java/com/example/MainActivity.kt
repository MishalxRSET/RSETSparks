package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppConfig
import com.example.data.PushRegistrar
import com.example.data.repository.AppRepository
import com.example.notifications.AndroidPushRegistrar
import com.example.notifications.Notifications
import com.example.security.AndroidGoogleSignIn
import com.example.security.AuthService
import com.example.ui.RSETSparksApp
import com.example.ui.viewmodel.RSETSparksViewModel
import com.example.ui.viewmodel.SessionState
import com.google.firebase.FirebaseApp

/** Android host for the shared app (shared/): wires in the Android Firebase SDK, Google picker and FCM. */
class MainActivity : ComponentActivity() {
    private val appConfig = AppConfig(isBlaze = BuildConfig.BLAZE, versionName = BuildConfig.VERSION_NAME)

    private val viewModel: RSETSparksViewModel by viewModels {
        viewModelFactory {
            initializer {
                // FirebaseApp is missing when the build has no google-services.json; the app then shows a setup screen.
                val firebaseReady = FirebaseApp.getApps(applicationContext).isNotEmpty()
                val repository = if (firebaseReady) AppRepository() else null
                RSETSparksViewModel(
                    auth = if (firebaseReady) AuthService(applicationContext) else null,
                    repository = repository,
                    push = repository?.let { AndroidPushRegistrar(applicationContext, it) } ?: PushRegistrar.None,
                    config = appConfig
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the branded system splash up until we know whether the student is signed in.
        splash.setKeepOnScreenCondition { viewModel.session.value is SessionState.Loading }
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        val googleSignIn = AndroidGoogleSignIn(this)
        setContent {
            RSETSparksApp(
                viewModel = viewModel,
                config = appConfig,
                googleSignIn = googleSignIn::requestTokens,
                requestNotificationPermission = {
                    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                },
                onChatVisibilityChanged = { partnerId ->
                    Notifications.activeChatPartnerId = partnerId
                    if (partnerId != null) NotificationManagerCompat.from(this).cancel("message:$partnerId", 0)
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val partnerId = intent?.getStringExtra(Notifications.EXTRA_OPEN_CHAT_WITH) ?: return
        intent.removeExtra(Notifications.EXTRA_OPEN_CHAT_WITH)
        viewModel.openChatWith(partnerId)
    }
}
