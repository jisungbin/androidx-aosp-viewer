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

  fun assert(asserter: context(TestLogger) Asserter.() -> Unit) {
    asserter.invoke(this, TestLoggerAsserter())
    assertAll {
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
  }

  @TestDsl interface Asserter {
    infix fun List<String>.has(text: String)
  }
}