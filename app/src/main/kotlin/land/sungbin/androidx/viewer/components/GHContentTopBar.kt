/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer.components

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
import kotlinx.collections.immutable.ImmutableList
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.viewer.R

@Composable fun GHContentTopBar(
  contents: ImmutableList<GitContent>,
  canNavigateBack: Boolean = true,
  modifier: Modifier = Modifier,
  onBackClick: () -> Unit = {},
  onCopyClick: () -> Unit = {},
  onSettingClick: () -> Unit = {},
) {
  val currentPath = remember(contents) {
    contents.firstOrNull()?.parent?.firstOrNull()?.path
  }
  val wholePaths = remember(contents) {
    contents.firstOrNull()?.parent?.firstOrNull()
      ?.wholePaths()
      ?.asReversed()
      ?.joinToString("/", prefix = "/")
  }

  TopAppBar(
    modifier = modifier,
    title = {
      if (contents.isNotEmpty()) {
        Column {
          Text(
            text = currentPath ?: "root",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            modifier = Modifier.paddingFromBaseline(top = 6.dp),
            text = wholePaths ?: "/",
            style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray),
          )
        }
      }
    },
    navigationIcon = {
      if (canNavigateBack) {
        IconButton(onClick = onBackClick) {
          Icon(
            painter = painterResource(R.drawable.ic_round_arrow_back_24),
            contentDescription = "Back",
          )
        }
      }
    },
    actions = {
      if (contents.isNotEmpty()) {
        IconButton(onClick = onCopyClick) {
          Icon(
            painter = painterResource(R.drawable.ic_round_content_copy_24),
            contentDescription = "Copy",
          )
        }
      }
      IconButton(onClick = onSettingClick) {
        Icon(
          painter = painterResource(R.drawable.ic_round_settings_24),
          contentDescription = "Settings",
        )
      }
    },
  )
}

private fun GitContent.wholePaths(builder: MutableList<String> = mutableListOf()): List<String> {
  builder += path
  return parent?.firstOrNull()?.wholePaths(builder) ?: builder
}
