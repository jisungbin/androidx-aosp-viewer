// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import kotlin.test.Test

class GitContentTest {
  @Test fun pathMustNotBeEmpty() {
    assertFailure {
      GitContent("", "url", null, null)
    }
      .hasMessage("path should not be empty")
  }

  @Test fun urlMustNotBeEmpty() {
    assertFailure {
      GitContent("path", "", null, null)
    }
      .hasMessage("url should not be empty")
  }

  @Test fun paths() {
    val parent = GitContent("parent", "url", null)
    val child = GitContent("child", "url", null, parent)
    val grandChild = GitContent("grandChild", "url", null, child)

    assertThat(child.paths).isEqualTo("parent/child")
    assertThat(grandChild.paths).isEqualTo("parent/child/grandChild")
  }
}
