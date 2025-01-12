// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable public data class AndroidxRepositoryTree(
  public val truncated: Boolean,
  private val tree: List<GitContent>,
) : ImmutableList<GitContent> by tree.toImmutableList() {
  public companion object {
    public val Empty: AndroidxRepositoryTree =
      AndroidxRepositoryTree(truncated = false, tree = emptyList())
  }
}
