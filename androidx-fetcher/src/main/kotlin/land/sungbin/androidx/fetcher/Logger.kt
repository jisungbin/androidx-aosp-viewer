// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import java.util.logging.Level
import java.util.logging.Logger as JavaLogger

// Timber is an Android library, so we can't use it.
public open class Logger {
  protected open val logger: JavaLogger by lazy {
    JavaLogger.getLogger(Logger::class.qualifiedName!!).apply {
      level = Level.ALL
    }
  }

  public open fun debug(lazyMessage: () -> String) {
    if (logger.isLoggable(Level.INFO)) {
      logger.info(lazyMessage())
    }
  }

  public open fun warn(lazyMessage: () -> String) {
    if (logger.isLoggable(Level.WARNING)) {
      logger.warning(lazyMessage())
    }
  }

  public open fun error(lazyMessage: () -> String) {
    if (logger.isLoggable(Level.SEVERE)) {
      logger.severe(lazyMessage())
    }
  }

  public companion object Default : Logger() {
    override fun toString(): String = "DefaultLogger"
  }
}
