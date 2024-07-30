/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import timber.log.Timber

class MainActivity : ComponentActivity() {
  private val ghLogin = GitHubLogin()
  private val accessToken by lazy { ghLogin.readAccessTokenFromStorage(applicationContext, FileSystem.SYSTEM) }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        if (accessToken == null && ghLogin.isLoginNeeded()) {
          Button(onClick = { ghLogin.login(this@MainActivity) }) {
            Text("Login with GitHub")
          }
        } else if (accessToken != null) {
          Text("Logged in with GitHub: $accessToken")
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val redirectUri = intent.data ?: return
    if (redirectUri.scheme == "androidx-aosp-viewer" && redirectUri.host == "github-login") {
      lifecycleScope.launch {
        val token = withContext(Dispatchers.IO) { ghLogin.requestAccessTokenFromRedirectUri(redirectUri) }
        Timber.d("GitHub AccessToken: %s", token)

        token.getOrNull()?.let { ghLogin.writeAccessTokenToStorage(applicationContext, FileSystem.SYSTEM, it) }
      }
    }
  }
}
