/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

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
