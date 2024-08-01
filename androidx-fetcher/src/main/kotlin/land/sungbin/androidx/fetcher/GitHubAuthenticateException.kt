/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer.exception

import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import okhttp3.Response

public data class GitHubAuthenticateException(
  val code: Int,
  override val message: String,
) : IOException() {
  internal companion object {
    fun parse(response: Response): GitHubAuthenticateException? {
      // https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api?apiVersion=2022-11-28#failed-login-limit
      if (
        response.code == HTTP_UNAUTHORIZED ||
        response.code == HTTP_FORBIDDEN ||
        response.code == HTTP_NOT_FOUND
      ) {
        return GitHubAuthenticateException(response.code, response.message)
      }
      return null
    }
  }
}
