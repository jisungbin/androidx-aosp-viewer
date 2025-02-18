// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import androidx.compose.runtime.Immutable
import kotlin.contracts.contract

@Immutable public sealed interface GitItem {
  public val parent: Tree?

  @Immutable public data class Tree(
    public val tree: AndroidxRepositoryTree,
    override val parent: Tree?,
  ) : GitItem

  @Immutable public data class Blob(
    public val raw: String,
    public val content: GitContent,
    override val parent: Tree?,
  ) : GitItem {
    init {
      requireNotNull(content.size) { "size should not be null" }
    }
  }
}

public val GitItem.paths: String
  get() = buildString {
    var parent = parent
    while (parent != null) {
      val firstContent = parent.firstContentOrNull() ?: break
      insert(0, "${firstContent.path}/")
      parent = parent.parent
    }
    append(firstContentOrNull()?.path.orEmpty())
  }

public fun GitItem.firstContentOrNull(): GitContent? =
  if (isBlob()) content else tree.contents.firstOrNull()

public val GitItem.isRoot: Boolean
  inline get() = parent == null

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
