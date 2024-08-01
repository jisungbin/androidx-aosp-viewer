/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

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

  fun clear() {
    debugs.clear()
    warns.clear()
    errors.clear()
  }
}
