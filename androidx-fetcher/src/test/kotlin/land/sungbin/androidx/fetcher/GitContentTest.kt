// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf

class GitContentTest {
  @Test fun pathMustNotBeEmpty() {
    assertFailure {
      GitContent("", "url", null, persistentListOf())
    }
      .hasMessage("path should not be empty")
  }

  @Test fun urlMustNotBeEmpty() {
    assertFailure {
      GitContent("path", "", null, persistentListOf())
    }
      .hasMessage("url should not be empty")
  }

  @Test fun currentParentPath() {
    val parent = GitContent("parent", "url", null, persistentListOf())
    val child = GitContent("child", "url", null, persistentListOf(parent))

    assertThat(child.currentParentPath()).isEqualTo("parent")
  }

  @Test fun wholeParentPaths() {
    val parent = GitContent("parent", "url", null, persistentListOf())
    val parent2 = GitContent("parent2", "url", null, persistentListOf(parent))
    val parent3 = GitContent("parent3", "url", null, persistentListOf(parent2))
    val parent4 = GitContent("parent4", "url", null, persistentListOf(parent3))
    val child = GitContent("child", "url", null, persistentListOf(parent4))

    assertThat(child.wholeParentPaths()).isEqualTo("/parent/parent2/parent3/parent4")
  }
}
