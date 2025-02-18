// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import java.util.logging.Level
import kotlinx.coroutines.channels.Channel
import thirdparty.Timber

class TimberAppTree(private val debug: Boolean) : Timber.DebugTree() {
  override fun log(level: Level, tag: String?, message: String, t: Throwable?) {
    if (level == Level.SEVERE)
      exceptions.trySend(Exception(message, t))
    else if (debug)
      super.log(level, tag, message, t)
  }

  companion object {
    val exceptions = Channel<Exception>(Channel.CONFLATED)
  }
}
