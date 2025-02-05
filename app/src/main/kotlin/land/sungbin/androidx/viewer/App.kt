// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import android.app.Application
import kotlinx.coroutines.runBlocking
import land.sungbin.androidx.fetcher.AndroidxRepositoryReader
import land.sungbin.androidx.viewer.presenter.AndroidxRepositoryReader
import thirdparty.Timber

class App : Application() {
  override fun onCreate() {
    super.onCreate()

    // TODO plant release tree before app release
    Timber.plant(Timber.DebugTree())
    preloadedRepoReader = runBlocking { AndroidxRepositoryReader(applicationContext) }
  }

  companion object {
    lateinit var preloadedRepoReader: AndroidxRepositoryReader
  }
}
