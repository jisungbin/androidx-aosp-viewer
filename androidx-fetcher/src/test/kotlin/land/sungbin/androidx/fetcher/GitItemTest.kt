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
  @Test fun requiresSizeForBlobType() {
    assertFailure {
      GitItem.Blob(GitContent("name", "url", size = null))
    }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("size should not be null")
  }

  @Test fun firstContentOrNull() {
    val contents = List(2) {
      GitContent("name $it", "url $it", it.toLong(), parent = null)
    }
    val tree = GitItem.Tree(contents.toImmutableList())
    val blob = GitItem.Blob(contents.last())

    assertThat(tree.firstContentOrNull(), name = "tree").isEqualTo(contents.first())
    assertThat(blob.firstContentOrNull(), name = "blob").isEqualTo(contents.last())
  }

  @Suppress("KotlinConstantConditions")
  @Test fun isBlob() {
    val blob = GitItem.Blob(GitContent("name", "url", 0L, parent = null))
    assertThat(blob.isBlob()).isTrue()
    assertThat(blob.isTree()).isFalse()
  }

  @Suppress("KotlinConstantConditions")
  @Test fun isTree() {
    val tree = GitItem.Tree(persistentListOf())
    assertThat(tree.isBlob()).isFalse()
    assertThat(tree.isTree()).isTrue()
  }
}
