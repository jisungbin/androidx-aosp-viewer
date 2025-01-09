package land.sungbin.androidx.viewer.screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.StaticScreen
import dev.drewhamilton.poko.Poko
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize
import land.sungbin.androidx.fetcher.GitContent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@Parcelize data object BrowseScreen : StaticScreen

sealed interface BrowseEvent : CircuitUiEvent {
  @Poko class Fetch(val parent: GitContent?, val noCache: Boolean) : BrowseEvent
  @Poko class ToggleFavorite(val content: GitContent) : BrowseEvent
}

@Immutable data class BrowseState(val tree: ImmutableList<GitContent>) : CircuitUiState

@CircuitInject(BrowseScreen::class, AppScope::class)
@Composable fun Browses(modifier: Modifier = Modifier) {
  Text("Browses!", modifier = modifier)
}
