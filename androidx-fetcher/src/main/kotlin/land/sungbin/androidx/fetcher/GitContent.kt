// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import okio.ByteString

@Immutable
public data class GitContent(
  public val path: String,
  public val url: String,
  public val blob: ByteString?,
  public val parent: ImmutableList<GitContent>? = null,
) {
  init {
    require(path.isNotEmpty()) { "path should not be empty" }
    require(url.isNotEmpty()) { "url should not be empty" }
  }

  public fun currentParentPath(): String? = parent?.firstOrNull()?.path

  public fun wholeParentPaths(): String? {
    fun GitContent.wholePaths(builder: MutableList<String> = mutableListOf()): List<String> {
      builder += path
      return parent?.firstOrNull()?.wholePaths(builder) ?: builder
    }
    return parent?.firstOrNull()
      ?.wholePaths()
      ?.asReversed()
      ?.joinToString("/", prefix = "/")
  }
}
