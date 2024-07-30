/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer.exception

import java.io.IOException

// TODO
//  unit testing
//  connection error handling
public data class AuthenticateException(
  val code: Int,
  override val message: String,
) : IOException() {
  init {
    require(code in 400..499) { "code must be in 400..499" }
  }
}
