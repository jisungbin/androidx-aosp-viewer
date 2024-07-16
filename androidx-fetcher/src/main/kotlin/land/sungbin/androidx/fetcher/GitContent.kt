package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class GitContent(
  public val path: String,
  public val url: String,
  public val blob: GitBlob?,
  public val children: ImmutableList<GitContent>,
)
