// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import land.sungbin.androidx.fetcher.AndroidxRepositoryTree
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.fetcher.isDirectory
import land.sungbin.androidx.viewer.R

@Composable fun GHContentTree(
  tree: AndroidxRepositoryTree,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  onContentClick: (content: GitContent) -> Unit = {},
) {
  LaunchedEffect(tree.contents) {
    listState.scrollToItem(0)
  }

  LazyColumn(modifier = modifier, state = listState) {
    items(tree.contents) { content ->
      GHContent(
        content,
        modifier = Modifier.fillMaxWidth(),
        onClick = { onContentClick(content) },
      )
    }
    if (tree.truncated) {
      item {
        Text(
          stringResource(R.string.screen_code_tree_truncated),
          modifier = Modifier
            .padding(all = 30.dp)
            .fillMaxSize()
            .wrapContentSize(),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable private fun GHContent(
  content: GitContent,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  if (content.isDirectory) {
    Text(
      content.path,
      modifier = modifier
        .clickable(onClick = onClick)
        .padding(vertical = 8.dp, horizontal = 16.dp),
      style = MaterialTheme.typography.titleMedium,
    )
  } else {
    val size = requireNotNull(content.size) { "size must not be null" }

    Column(
      modifier = modifier
        .clickable(onClick = onClick)
        .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
      Text(content.path, style = MaterialTheme.typography.titleMedium)
      Text(
        "%.2f KB".format(size.toFloat() / 1_000f),
        modifier = Modifier.padding(top = 10.dp),
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}
