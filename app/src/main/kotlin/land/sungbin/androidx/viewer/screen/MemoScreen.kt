package land.sungbin.androidx.viewer.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.screen.StaticScreen
import kotlinx.parcelize.Parcelize
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@Parcelize data object MemoScreen : StaticScreen

@CircuitInject(MemoScreen::class, AppScope::class)
@Composable fun Memos(modifier: Modifier = Modifier) {
  Text("Memos!", modifier = modifier)
}
