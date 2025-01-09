// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import dev.drewhamilton.poko.Poko
import java.io.IOException
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import okhttp3.Response

@Poko public class GitHubAuthenticateException(
  public val code: Int,
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
