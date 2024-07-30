package land.sungbin.androidx.viewer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import land.sungbin.androidx.fetcher.GitContent

@Composable fun GHContentTopBar(
  contents: PersistentList<GitContent>,
  modifier: Modifier = Modifier,
  onBackClick: () -> Unit = {},
  onPlusClick: () -> Unit = {},
  onMinusClick: () -> Unit = {},
  onRefreshClick: () -> Unit = {},
  onCopyClick: () -> Unit = {},
) {
  val currentPath = remember(contents) { contents.last().path }
  val wholePaths = remember(contents) { contents.joinToString("/") }

  TopAppBar(
    modifier = modifier,
    title = {
      Column {
        Text(
          text = currentPath,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          modifier = Modifier.paddingFromBaseline(top = 6.dp),
          text = wholePaths,
          style = MaterialTheme.typography.labelMedium.copy(color = Color.LightGray),
        )
      }
    },
  )
}