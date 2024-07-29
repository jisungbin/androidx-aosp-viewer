/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertions.hasMessage
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestLoggerAsserterTest {
  private lateinit var logger: TestLogger

  @BeforeTest fun prepare() {
    logger = TestLogger()
  }

  @Test fun assertContainsDebugMessage() {
    logger.debug { "Hello, World!" }
    logger.assert { debugs has "Hello, World!" }

    assertFailure { logger.assert { debugs has "Hello, World!" } }
      .hasMessage("Expected to find Hello, World! in []")
  }

  @Test fun assertNotContainsDebugMessage() {
    logger.debug { "Hello, World!" }

    assertFailure { logger.assert { debugs hasNot "Hello, World!" } }
      .hasMessage("Expected not to find Hello, World! in [Hello, World!]")

    logger.assert(mustAssertAll = false) { debugs hasNot "Hello, World!!" }
  }

  @Test fun assertContainsWarnMessage() {
    logger.warn { "Hello, World!" }
    logger.assert { warns has "Hello, World!" }

    assertFailure { logger.assert { warns has "Hello, World!" } }
      .hasMessage("Expected to find Hello, World! in []")
  }

  @Test fun assertNotContainsWarnMessage() {
    logger.warn { "Hello, World!" }

    assertFailure { logger.assert { warns hasNot "Hello, World!" } }
      .hasMessage("Expected not to find Hello, World! in [Hello, World!]")

    logger.assert(mustAssertAll = false) { warns hasNot "Hello, World!!" }
  }

  @Test fun assertContainsErrorMessage() {
    logger.error { "Hello, World!" }
    logger.assert { errors has "Hello, World!" }

    assertFailure { logger.assert { errors has "Hello, World!" } }
      .hasMessage("Expected to find Hello, World! in []")
  }

  @Test fun assertNotContainsErrorMessage() {
    logger.error { "Hello, World!" }

    assertFailure { logger.assert { errors hasNot "Hello, World!" } }
      .hasMessage("Expected not to find Hello, World! in [Hello, World!]")

    logger.assert(mustAssertAll = false) { errors hasNot "Hello, World!!" }
  }

  @Test fun assertNoUncheckedMessagesWhenMustAssertAll() {
    logger.debug { "Hello, Debug!" }
    logger.debug { "Hello, Debug! - 2" }
    logger.warn { "Hello, Warn!" }
    logger.warn { "Hello, Warn! - 2" }
    logger.error { "Hello, Error!" }
    logger.error { "Hello, Error! - 2" }

    assertFailure {
      logger.assert(mustAssertAll = true) {
        debugs has "Hello, Debug!"
        warns has "Hello, Warn!"
        errors has "Hello, Error!"
      }
    }
      .hasMessage(
        """
The following assertions failed (3 failures)
	org.opentest4j.AssertionFailedError: expected [debugs] to be empty but was:<["Hello, Debug! - 2"]>
	org.opentest4j.AssertionFailedError: expected [warns] to be empty but was:<["Hello, Warn! - 2"]>
	org.opentest4j.AssertionFailedError: expected [errors] to be empty but was:<["Hello, Error! - 2"]>
        """.trimIndent(),
      )
  }

  @Test fun assertHaveUncheckedMessagesWhenMustntAssertAll() {
    logger.debug { "Hello, Debug!" }
    logger.debug { "Hello, Debug! - 2" }
    logger.warn { "Hello, Warn!" }
    logger.warn { "Hello, Warn! - 2" }
    logger.error { "Hello, Error!" }
    logger.error { "Hello, Error! - 2" }

    logger.assert(mustAssertAll = false) {
      debugs has "Hello, Debug!"
      warns has "Hello, Warn!"
      errors has "Hello, Error!"
    }
  }
}
