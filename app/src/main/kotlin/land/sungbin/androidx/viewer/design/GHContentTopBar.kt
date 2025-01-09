// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.fetcher.paths
import land.sungbin.androidx.viewer.R

@Composable fun GHContentTopBar(
  content: GitContent,
  modifier: Modifier = Modifier,
  onBackClick: (() -> Unit)? = null,
  onRefresh: (() -> Unit)? = null,
  onCopy: (() -> Unit)? = null,
) {
  val contentPaths = remember(content, content::paths)

  TopAppBar(
    modifier = modifier,
    title = {
      Column {
        Text(
          content.path,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          contentPaths,
          modifier = Modifier.paddingFromBaseline(top = 6.dp),
          style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray),
        )
      }
    },
    navigationIcon = {
      if (onBackClick != null) {
        IconButton(onClick = onBackClick) {
          Icon(painterResource(R.drawable.ic_round_arrow_back_24), "Back")
        }
      }
    },
    actions = {
      if (onCopy != null) {
        IconButton(onClick = onCopy) {
          Icon(painterResource(R.drawable.ic_round_content_copy_24), "Copy")
        }
      }
      if (onRefresh != null) {
        // TODO swipe to refresh
        IconButton(onClick = onRefresh) {
          Icon(painterResource(R.drawable.ic_round_refresh_24), "Refresh")
        }
      }
    },
  )
}
