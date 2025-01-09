// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.design

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import land.sungbin.androidx.fetcher.GitContent

@Composable fun GHContentTree(
  contents: ImmutableList<GitContent>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  onContentClick: (content: GitContent) -> Unit = {},
) {
  LazyColumn(modifier = modifier, state = listState) {
    items(contents, key = GitContent::url) { content ->
      if (content.isDirectory) {
        GHFolder(
          content.path,
          modifier = Modifier.fillMaxWidth(),
          onClick = { onContentClick(content) },
        )
      } else {
        GHBlob(
          content,
          modifier = Modifier.fillMaxWidth(),
          onClick = { onContentClick(content) },
        )
      }
    }
  }
}

@Composable private fun GHFolder(
  name: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  Text(
    name,
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 16.dp),
    style = MaterialTheme.typography.titleMedium,
  )
}

@Composable private fun GHBlob(
  content: GitContent,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val blob = requireNotNull(content.blob) { "blob must not be null" }

  Column(
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 16.dp),
  ) {
    Text(content.path, style = MaterialTheme.typography.titleMedium)
    Text(
      "%.2f KB".format(blob.size.toFloat() / 1_000),
      modifier = Modifier.paddingFromBaseline(top = 20.dp),
      style = MaterialTheme.typography.bodySmall,
    )
  }
}
