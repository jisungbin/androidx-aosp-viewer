// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import okio.ByteString

@Immutable public data class GitContent(
  public val path: String,
  public val url: String,
  public val blob: ByteString?,
  public val parent: GitContent? = null,
) {
  init {
    require(path.isNotEmpty()) { "path should not be empty" }
    require(url.isNotEmpty()) { "url should not be empty" }
  }

  public val isFile: Boolean get() = blob != null
  public val isDirectory: Boolean get() = blob == null
}

@Stable public val GitContent.paths: String
  get() = buildString {
    var parent = parent
    while (parent != null) {
      insert(0, "${parent.path}/")
      parent = parent.parent
    }
    append(path)
  }
