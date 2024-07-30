/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer.utils

import land.sungbin.androidx.fetcher.Logger
import timber.log.Timber

class TimberLogger(private val tag: String) : Logger() {
  override fun debug(lazyMessage: () -> String) {
    Timber.tag(tag).d(lazyMessage())
  }

  override fun warn(lazyMessage: () -> String) {
    Timber.tag(tag).w(lazyMessage())
  }

  override fun error(lazyMessage: () -> String) {
    Timber.tag(tag).e(lazyMessage())
  }
}
