/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEmpty

class TestLogger : Logger() {
  val debugs: List<String>
    field = mutableListOf()

  val warns: List<String>
    field = mutableListOf()

  val errors: List<String>
    field = mutableListOf()

  override fun debug(lazyMessage: () -> String) {
    debugs += lazyMessage()
  }

  override fun warn(lazyMessage: () -> String) {
    warns += lazyMessage()
  }

  override fun error(lazyMessage: () -> String) {
    errors += lazyMessage()
  }

  fun assert(
    mustAssertAll: Boolean = true,
    asserter: context(TestLogger)
    Asserter.() -> Unit,
  ) {
    asserter.invoke(this, TestLoggerAsserter())
    if (mustAssertAll) assertAll {
      assertThat(debugs, "debugs").isEmpty()
      assertThat(warns, "warns").isEmpty()
      assertThat(errors, "errors").isEmpty()
    }
  }

  @Suppress("TestFunctionName")
  private fun TestLoggerAsserter() = object : Asserter {
    override fun List<String>.has(text: String) {
      if (!(this as MutableList<String>).removeIf { it == text }) {
        throw AssertionError("Expected to find $text in $this")
      }
    }

    override fun List<String>.hasNot(text: String) {
      if (contains(text)) {
        throw AssertionError("Expected not to find $text in $this")
      }
    }
  }

  @TestDsl interface Asserter {
    infix fun List<String>.has(text: String)
    infix fun List<String>.hasNot(text: String)
  }
}
