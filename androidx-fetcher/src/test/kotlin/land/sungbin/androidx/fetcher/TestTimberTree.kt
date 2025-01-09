// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import java.util.logging.Level
import thirdparty.Timber

class TestTimberTree : Timber.Tree() {
  val debugs: List<String> field = mutableListOf()
  val warns: List<String> field = mutableListOf()
  val errors: List<String> field = mutableListOf()

  override fun log(level: Level, tag: String?, message: String, t: Throwable?) {
    when (level) {
      Level.INFO -> debugs += message
      Level.WARNING -> warns += message
      Level.SEVERE -> errors += message
    }
  }

  fun clear() {
    debugs.clear()
    warns.clear()
    errors.clear()
  }
}
