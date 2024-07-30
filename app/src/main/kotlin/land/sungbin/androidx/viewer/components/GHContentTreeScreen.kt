package land.sungbin.androidx.viewer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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

@Composable fun GHContentTreeScreen(
  contents: ImmutableList<GitContent>,
  modifier: Modifier = Modifier,
  listState: LazyListState = rememberLazyListState(),
  onContentClick: (content: GitContent) -> Unit = {},
) {
  LazyColumn(modifier = modifier, state = listState) {
    items(contents, key = GitContent::url) { content ->
      if (content.blob == null) {
        GHFolder(
          modifier = Modifier.fillMaxWidth(),
          name = content.path,
          onClick = { onContentClick(content) },
        )
      } else {
        GHBlob(
          modifier = Modifier.fillMaxWidth(),
          content = content,
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
    modifier = modifier
      .padding(vertical = 8.dp, horizontal = 16.dp)
      .clickable(onClick = onClick),
    text = name,
    style = MaterialTheme.typography.titleMedium,
  )
}

@Composable private fun GHBlob(
  content: GitContent,
  modifier: Modifier = Modifier,
  onClick: () -> Unit = {},
) {
  val blob = requireNotNull(content.blob) { "blob must not be null" }

  Row(
    modifier = modifier
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 16.dp),
  ) {
    Text(text = content.path, style = MaterialTheme.typography.titleMedium)
    Text(
      modifier = Modifier.paddingFromBaseline(top = 20.dp),
      text = "${blob.size / 1_000} KB",
      style = MaterialTheme.typography.bodySmall,
    )
  }
}
