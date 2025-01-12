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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import land.sungbin.androidx.fetcher.AndroidxRepository
import land.sungbin.androidx.fetcher.GitItem
import land.sungbin.androidx.fetcher.firstContentOrNull
import land.sungbin.androidx.fetcher.isBlob
import land.sungbin.androidx.fetcher.isRoot
import land.sungbin.androidx.fetcher.paths
import land.sungbin.androidx.viewer.R

@Composable fun EmptyTopBar(modifier: Modifier = Modifier) {
  TopAppBar(
    modifier = modifier,
    title = {
      Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.titleLarge,
      )
    },
  )
}

@Composable fun GHContentTopBar(
  item: GitItem,
  modifier: Modifier = Modifier,
  onBackClick: (() -> Unit)? = null,
  onRefresh: (() -> Unit)? = null,
  onOpenWeb: (() -> Unit)? = null,
) {
  val firstContent = item.firstContentOrNull()

  TopAppBar(
    modifier = modifier,
    title = {
      Column {
        Text(
          if (item.isBlob()) firstContent!!.path else firstContent?.parent?.path ?: AndroidxRepository.HOME_REF,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
        )
        if (firstContent?.isRoot == false) {
          Text(
            "/${firstContent.parent!!.paths}",
            modifier = Modifier.paddingFromBaseline(top = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray),
            maxLines = 1,
          )
        }
      }
    },
    navigationIcon = {
      if (onBackClick != null) {
        IconButton(onClick = onBackClick) {
          Icon(painterResource(R.drawable.ic_fill_arrow_back_24), "Navigate Back")
        }
      }
    },
    actions = {
      if (onOpenWeb != null) {
        IconButton(onClick = onOpenWeb) {
          Icon(painterResource(R.drawable.ic_outline_open_in_new_24), "Open Web")
        }
      }
      if (onRefresh != null) {
        // TODO swipe to refresh
        IconButton(onClick = onRefresh) {
          Icon(painterResource(R.drawable.ic_fill_refresh_24), "Refresh")
        }
      }
    },
  )
}
