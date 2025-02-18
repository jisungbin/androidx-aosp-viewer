// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable

@Immutable public data class GitContent(
  public val path: String,
  public val url: String,
  public val size: Long?,
) {
  init {
    require(path.isNotEmpty()) { "path should not be empty" }
    require(url.isNotEmpty()) { "url should not be empty" }
  }
}

public val GitContent.sha: String
  get() =
    url
      .substringAfterLast('/', missingDelimiterValue = AndroidxRepository.HOME_REF)
      .ifEmpty { AndroidxRepository.HOME_REF }

public val GitContent.isFile: Boolean
  get() = size != null

public val GitContent.isDirectory: Boolean
  get() = size == null
