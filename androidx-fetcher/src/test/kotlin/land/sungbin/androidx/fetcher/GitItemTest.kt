// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class GitItemTest {
  @Test fun requiresContentSizeForBlob() {
    assertFailure { GitItem.Blob("", GitContent("name", "url", size = null), parent = null) }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("size should not be null")
  }

  @Test fun firstContentOrNull() {
    val contents = List(2) { GitContent("name $it", "url $it", it.toLong()) }

    val tree = GitItem.Tree(AndroidxRepositoryTree("sha", false, contents.toImmutableList()), parent = null)
    val blob = GitItem.Blob("", contents.last(), parent = null)

    assertThat(tree.firstContentOrNull(), name = "tree").isEqualTo(contents.first())
    assertThat(blob.firstContentOrNull(), name = "blob").isEqualTo(contents.last())
  }

  @Suppress("KotlinConstantConditions")
  @Test fun isBlob() {
    val blob = GitItem.Blob("", GitContent("name", "url", 0L), parent = null)

    assertThat(blob.isBlob()).isTrue()
    assertThat(blob.isTree()).isFalse()
  }

  @Suppress("KotlinConstantConditions")
  @Test fun isTree() {
    val tree = GitItem.Tree(AndroidxRepositoryTree.Empty, parent = null)

    assertThat(tree.isBlob()).isFalse()
    assertThat(tree.isTree()).isTrue()
  }

  @Test fun isRootWhenParentIsNull() {
    val tree = GitItem.Tree(AndroidxRepositoryTree.Empty, parent = null)

    assertThat(tree.isRoot, name = "be root").isTrue()
    assertThat(GitItem.Tree(AndroidxRepositoryTree.Empty, parent = tree).isRoot, name = "be not root").isFalse()
  }

  @Test fun retrievesFullPathForNestedContents() {
    fun gitTree(name: String, parent: GitItem.Tree? = null) =
      GitItem.Tree(
        AndroidxRepositoryTree("sha", false, persistentListOf(GitContent(name, url = "url", size = null))),
        parent,
      )

    val root = gitTree("root")
    val child = gitTree("child", root)
    val grandchild = gitTree("grandchild", child)

    assertThat(root.paths).isEqualTo("root")
    assertThat(child.paths).isEqualTo("root/child")
    assertThat(grandchild.paths).isEqualTo("root/child/grandchild")
  }
}
