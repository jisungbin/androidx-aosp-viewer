// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.screen

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.StaticScreen
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import land.sungbin.androidx.fetcher.AndroidxRepository
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.fetcher.GitItem
import land.sungbin.androidx.fetcher.firstContentOrNull
import land.sungbin.androidx.fetcher.isBlob
import land.sungbin.androidx.fetcher.isDirectory
import land.sungbin.androidx.fetcher.isRoot
import land.sungbin.androidx.fetcher.isTree
import land.sungbin.androidx.fetcher.paths
import land.sungbin.androidx.fetcher.sha
import land.sungbin.androidx.viewer.ui.DefaultTopBar
import land.sungbin.androidx.viewer.ui.GHContentLoading
import land.sungbin.androidx.viewer.ui.GHContentTopBar
import land.sungbin.androidx.viewer.ui.GHContentTree
import land.sungbin.androidx.viewer.util.conditionalLambda
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@Parcelize data object CodeScreen : StaticScreen {
  @Immutable data class State(
    val item: GitItem,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    @Poko class Init(val item: GitItem) : Event
    @Poko class Fetch(val sha: String, val parent: GitItem.Tree?, val noCache: Boolean = false) : Event
    @Poko class OpenBlob(val content: GitContent, val parent: GitItem.Tree?, val noCache: Boolean = false) : Event
    @Poko class ToggleFavorite(val content: GitContent) : Event
  }
}

@CircuitInject(CodeScreen::class, AppScope::class)
@Composable fun Codes(state: CodeScreen.State, modifier: Modifier = Modifier) {
  val host = LocalActivity.current!!
  val gitItem = rememberUpdatedState(state.item).value
  val firstContent = gitItem.firstContentOrNull()

  BackHandler(gitItem.parent != null) {
    state.eventSink(CodeScreen.Event.Init(gitItem.parent ?: return@BackHandler))
  }

  Column(modifier = modifier) {
    if (firstContent == null) {
      DefaultTopBar(modifier = Modifier.fillMaxWidth())
    } else {
      GHContentTopBar(
        modifier = Modifier.fillMaxWidth(),
        item = state.item,
        onBackClick = conditionalLambda(
          condition = { !gitItem.isRoot },
          LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher::onBackPressed,
        ),
        onRefresh = {
          if (gitItem.isTree())
            state.eventSink(CodeScreen.Event.Fetch(gitItem.tree.ref, parent = gitItem.parent, noCache = true))
          else
            state.eventSink(CodeScreen.Event.OpenBlob(gitItem.content, parent = gitItem.parent, noCache = true))
        },
        onOpenWeb = { host.startActivity(Intent(Intent.ACTION_VIEW, gitItem.githubLink().toUri())) },
      )
    }

    if (state.item.isTree() && state.item.tree.contents.isEmpty()) {
      GHContentLoading(
        modifier = Modifier
          .fillMaxSize()
          .wrapContentSize(),
      )
    } else {
      when (gitItem) {
        is GitItem.Tree -> {
          GHContentTree(
            gitItem.tree,
            modifier = Modifier.fillMaxSize(),
            onContentClick = { content ->
              if (content.isDirectory)
                state.eventSink(CodeScreen.Event.Fetch(content.sha, parent = gitItem.parent))
              else
                state.eventSink(CodeScreen.Event.OpenBlob(content, parent = gitItem.parent))
            },
          )
        }
        is GitItem.Blob -> {
          Text(gitItem.raw)
        }
      }
    }
  }
}

private fun GitItem.githubLink(): String =
  buildString {
    append("https://github.com/androidx/androidx/blob/")
    append(AndroidxRepository.HOME_REF)
    append('/')
    append(if (isBlob()) paths else parent?.paths.orEmpty())
  }
