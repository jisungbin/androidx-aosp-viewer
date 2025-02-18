// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable public data class AndroidxRepositoryTree(
  public val ref: String,
  public val truncated: Boolean,
  public val contents: ImmutableList<GitContent>,
) {
  init {
    require(ref.isNotEmpty()) { "Ref should not be empty" }
  }

  public companion object {
    public val Empty: AndroidxRepositoryTree =
      AndroidxRepositoryTree(ref = AndroidxRepository.HOME_REF, truncated = false, contents = persistentListOf())
  }
}
