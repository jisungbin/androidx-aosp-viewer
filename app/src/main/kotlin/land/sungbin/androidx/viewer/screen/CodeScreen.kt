// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State as ComposeState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.StaticScreen
import dev.drewhamilton.poko.Poko
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.Parcelize
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.fetcher.GitItem
import land.sungbin.androidx.fetcher.firstContentOrNull
import land.sungbin.androidx.fetcher.isBlob
import land.sungbin.androidx.fetcher.isDirectory
import land.sungbin.androidx.fetcher.isRoot
import land.sungbin.androidx.fetcher.isTree
import land.sungbin.androidx.viewer.ui.GHContentLoading
import land.sungbin.androidx.viewer.ui.GHContentTree
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Parcelize data object CodeScreen : StaticScreen {
  @Immutable data class State(
    val item: GitItem,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  // TODO Is this the best?
  @SingleIn(AppScope::class)
  @JvmInline value class SharedState(val state: ComposeState<GitItem>) {
    @Inject constructor() : this(mutableStateOf(GitItem.Tree(persistentListOf())))

    fun assign(item: GitItem) {
      check(state is MutableState) { "State is not mutable" }
      state.value = item
    }
  }

  sealed interface Event : CircuitUiEvent {
    @Poko class Fetch(val parent: GitContent?, val noCache: Boolean = false) : Event
    @Poko class OpenBlob(val content: GitContent, val noCache: Boolean = false) : Event
    @Poko class ToggleFavorite(val content: GitContent) : Event
  }
}

fun CodeScreen.SharedState.assignAsTree(contents: List<GitContent>) {
  assign(GitItem.Tree(contents.toImmutableList()))
}

fun CodeScreen.SharedState.assignAsBlob(raw: String, content: GitContent) {
  assign(GitItem.Blob(raw, content))
}

@CircuitInject(CodeScreen::class, AppScope::class)
@Composable fun Codes(state: CodeScreen.State, modifier: Modifier = Modifier) {
  val firstContent = state.item.firstContentOrNull()

  BackHandler(firstContent?.isRoot == false || state.item.isBlob() && firstContent?.isRoot == true) {
    state.eventSink(CodeScreen.Event.Fetch(parent = firstContent?.parent))
  }

  if (state.item.isTree() && state.item.contents.isEmpty()) {
    GHContentLoading(
      modifier = modifier
        .fillMaxSize()
        .wrapContentSize(),
    )
  } else {
    when (val item = state.item) {
      is GitItem.Tree -> {
        GHContentTree(
          item.contents,
          modifier = modifier.fillMaxSize(),
          onContentClick = { content ->
            if (content.isDirectory)
              state.eventSink(CodeScreen.Event.Fetch(parent = content))
            else
              state.eventSink(CodeScreen.Event.OpenBlob(content))
          },
        )
      }
      is GitItem.Blob -> {
        Text(item.raw)
      }
    }
  }
}
