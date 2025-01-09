package land.sungbin.androidx.viewer.overlay

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.slack.circuit.overlay.AnimatedOverlay
import com.slack.circuit.overlay.OverlayNavigator
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

class SnackbarOverlay(private val message: String) : AnimatedOverlay<Unit>(
  enterTransition = scaleIn() + fadeIn(),
  exitTransition = scaleOut() + fadeOut(),
) {
  @Composable override fun AnimatedVisibilityScope.AnimatedContent(navigator: OverlayNavigator<Unit>) {
    LaunchedEffect(Unit) {
      delay(2.seconds)
      navigator.finish(Unit)
    }

    Text(
      message,
      style = MaterialTheme.typography.bodyMedium.copy(color = SnackbarDefaults.contentColor),
      modifier = Modifier
        .fillMaxSize()
        .wrapContentHeight(Alignment.Bottom)
        .background(SnackbarDefaults.color, SnackbarDefaults.shape)
        .graphicsLayer { shadowElevation = 6.dp.toPx() }
        .padding(horizontal = 16.dp, vertical = 6.dp),
    )
  }
}
