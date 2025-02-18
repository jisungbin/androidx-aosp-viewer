// Copyright 2024 Ji Sungbin
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

class GitContentTest {
  @Test fun nameCannotBeEmpty() {
    assertFailure { GitContent("", "url", size = null) }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("path should not be empty")
  }

  @Test fun urlCannotBeEmpty() {
    assertFailure { GitContent("name", "", size = null) }
      .isInstanceOf<IllegalArgumentException>()
      .hasMessage("url should not be empty")
  }

  @Test fun retrievesShaFromUrl() {
    val content = GitContent(
      "name",
      "https://api.github.com/repos/androidx/androidx/git/trees/asdfasdasdasdasdasd123123123",
      size = null,
    )

    assertThat(content.sha).isEqualTo("asdfasdasdasdasdasd123123123")
  }

  @Test fun returnsDefaultRefWhenShaMissing() {
    val content = GitContent(
      "name",
      "https://api.github.com/repos/androidx/androidx/git/trees/",
      size = null,
    )

    assertThat(content.sha).isEqualTo(AndroidxRepository.HOME_REF)
  }

  @Test fun isFileWhenSizeIsNotNull() {
    val content = GitContent("name", "url", size = 0L)

    assertThat(content.isFile, name = "be file").isTrue()
    assertThat(content.isDirectory, name = "not be directory").isFalse()
  }

  @Test fun isDirectoryWhenSizeIsNull() {
    val content = GitContent("name", "url", size = null)

    assertThat(content.isFile, name = "not be file").isFalse()
    assertThat(content.isDirectory, name = "be directory").isTrue()
  }
}
