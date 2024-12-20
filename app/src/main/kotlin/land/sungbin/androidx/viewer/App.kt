// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import android.app.Application
import timber.log.Timber

class App : Application() {
  override fun onCreate() {
    super.onCreate()

    // TODO remove this line before release
    Timber.plant(Timber.DebugTree())
  }
}
