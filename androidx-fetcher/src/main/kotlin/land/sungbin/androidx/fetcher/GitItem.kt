// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import kotlin.contracts.contract
import kotlinx.collections.immutable.ImmutableList

@Immutable public sealed interface GitItem {
  @Immutable
  @JvmInline public value class Tree(public val contents: ImmutableList<GitContent>) : GitItem

  @Immutable public data class Blob(
    public val raw: String,
    public val content: GitContent,
  ) : GitItem {
    init {
      requireNotNull(content.size) { "size should not be null" }
    }
  }
}

public val GitItem.Blob.extension: String
  get() = content.path.substringAfterLast('.', missingDelimiterValue = "")

public fun GitItem.firstContentOrNull(): GitContent? =
  if (isBlob()) content else contents.firstOrNull()

public fun GitItem.isTree(): Boolean {
  contract {
    returns(true) implies (this@isTree is GitItem.Tree)
    returns(false) implies (this@isTree is GitItem.Blob)
  }
  return this is GitItem.Tree
}

public fun GitItem.isBlob(): Boolean {
  contract {
    returns(true) implies (this@isBlob is GitItem.Blob)
    returns(false) implies (this@isBlob is GitItem.Tree)
  }
  return this is GitItem.Blob
}
