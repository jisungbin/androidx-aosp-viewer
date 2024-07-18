package land.sungbin.androidx.fetcher

import java.util.logging.Level
import java.util.logging.Logger as JavaLogger

public open class Logger {
  private val logger by lazy {
    JavaLogger.getLogger(Logger::class.qualifiedName!!).apply {
      level = Level.ALL
    }
  }

  public open fun debug(lazyMessage: () -> String) {
    logger.info(lazyMessage)
  }

  public open fun warn(lazyMessage: () -> String) {
    logger.warning(lazyMessage)
  }

  public open fun error(lazyMessage: () -> String) {
    logger.severe(lazyMessage)
  }

  public companion object Default : Logger() {
    override fun toString(): String = "DefaultLogger"
  }
}
