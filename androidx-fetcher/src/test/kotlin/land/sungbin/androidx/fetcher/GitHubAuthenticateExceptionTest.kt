// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.all
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.jupiter.api.Test

class GitHubAuthenticateExceptionTest {
  @Test fun parsesHttpUnauthorizedResponse() {
    val response = responseOf(HTTP_UNAUTHORIZED, "Unauthorized")
    val exception = GitHubAuthenticateException.parse(response)

    assertThat(exception)
      .isNotNull()
      .all {
        prop(GitHubAuthenticateException::code).isEqualTo(HTTP_UNAUTHORIZED)
        prop(GitHubAuthenticateException::message).endsWith("Unauthorized")
      }
  }

  @Test fun parsesHttpForbiddenResponse() {
    val response = responseOf(HTTP_FORBIDDEN, "Forbidden")
    val exception = GitHubAuthenticateException.parse(response)

    assertThat(exception)
      .isNotNull()
      .all {
        prop(GitHubAuthenticateException::code).isEqualTo(HTTP_FORBIDDEN)
        prop(GitHubAuthenticateException::message).endsWith("Forbidden")
      }
  }

  @Test fun parsesHttpNotFoundResponse() {
    val response = responseOf(HTTP_NOT_FOUND, "Not Found")
    val exception = GitHubAuthenticateException.parse(response)

    assertThat(exception)
      .isNotNull()
      .all {
        prop(GitHubAuthenticateException::code).isEqualTo(HTTP_NOT_FOUND)
        prop(GitHubAuthenticateException::message).endsWith("Not Found")
      }
  }

  @Test fun doesNotParseDisallowedResponse() {
    val response = responseOf(HTTP_OK, "OK")
    val exception = GitHubAuthenticateException.parse(response)

    assertThat(exception).isNull()
  }

  private fun responseOf(code: Int, message: String): Response =
    Response.Builder()
      .code(code)
      .message(message)
      .request(Request.Builder().url("https://example.com").build())
      .protocol(Protocol.HTTP_1_1)
      .build()
}
