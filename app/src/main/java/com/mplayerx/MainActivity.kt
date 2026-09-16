package com.mplayerx

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mplayerx.ui.browser.BrowserScreen
import com.mplayerx.ui.home.HomeScreen
import com.mplayerx.ui.player.PlayerScreen
import com.mplayerx.ui.settings.SettingsScreen
import com.mplayerx.ui.theme.MPlayerXTheme
import java.net.URLDecoder
import java.net.URLEncoder

fun Uri.encodeForNav(): String = URLEncoder.encode(toString(), "UTF-8")
fun String.decodeToUri(): Uri = Uri.parse(URLDecoder.decode(this, "UTF-8"))

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestVideoPermissions()

        val app = application as MPlayerXApp
        val startDestination = "home"
        // VIEW intent → jump straight to player
        val viewUri: Uri? = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data

        setContent {
            MPlayerXTheme {
                val nav = rememberNavController()
                var launched by remember { mutableStateOf(false) }
                LaunchedEffect(viewUri) {
                    if (viewUri != null && !launched) {
                        launched = true
                        nav.navigate("player/${viewUri.encodeForNav()}?resume=-1")
                    }
                }
                NavHost(nav, startDestination) {
                    composable("home") {
                        HomeScreen(
                            app = app,
                            onOpenVideo = { uri, resume ->
                                nav.navigate("player/${uri.encodeForNav()}?resume=$resume")
                            },
                            onBrowse = { nav.navigate("browser") },
                            onSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("browser") {
                        BrowserScreen(
                            app = app,
                            onOpenVideo = { uri, resume ->
                                nav.navigate("player/${uri.encodeForNav()}?resume=$resume")
                            },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "player/{uri}?resume={resume}",
                        arguments = listOf(
                            navArgument("uri") { type = NavType.StringType },
                            navArgument("resume") { type = NavType.StringType; defaultValue = "-1" },
                        ),
                    ) { backStack ->
                        val uri = backStack.arguments!!.getString("uri")!!.decodeToUri()
                        val resumeArg = backStack.arguments!!.getString("resume")!!.toLongOrNull() ?: -1L
                        PlayerScreen(
                            app = app,
                            uri = uri,
                            resumeArgMs = resumeArg,
                            onBack = { nav.popBackStack() },
                            onEnterPip = { enterPip() },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(app = app, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        // PiP is entered from player controls; keep activity default otherwise.
        super.onUserLeaveHint()
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder().build()
            enterPictureInPictureMode(params)
        }
    }

    private fun requestVideoPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // PlayerScreen hides controls via activity PIP state observation if needed.
        pipMode = isInPictureInPictureMode
    }

    companion object {
        @Volatile var pipMode: Boolean = false
    }
}
