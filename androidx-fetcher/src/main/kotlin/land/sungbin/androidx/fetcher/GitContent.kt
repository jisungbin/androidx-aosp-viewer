package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable

@Immutable
public data class GitContent(
  public val path: String,
  public val url: String,
  public val blob: String?,
) {
  init {
    require(path.isNotEmpty()) { "path should not be empty" }
    require(url.isNotEmpty()) { "url should not be empty" }
  }
}
