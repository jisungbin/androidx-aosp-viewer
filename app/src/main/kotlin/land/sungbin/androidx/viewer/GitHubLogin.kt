// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.UiContext
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import com.squareup.moshi.JsonReader
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlinx.coroutines.suspendCancellableCoroutine
import land.sungbin.androidx.viewer.util.runSuspendCatching
import me.tatarka.inject.annotations.Inject
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.coroutines.executeAsync
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.BufferedSource
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import thirdparty.Timber

// TODO unit testing?
@SingleIn(AppScope::class)
@Inject class GitHubLogin {
  private val id = BuildConfig.GH_ID.orNull()
  private val secret = BuildConfig.GH_SECRET.orNull()

  private val client = OkHttpClient.Builder()
    .addInterceptor(
      HttpLoggingInterceptor { message -> Timber.tag(TAG).d(message) }
        .setLevel(HttpLoggingInterceptor.Level.BODY),
    )
    .build()

  private val loginRequestState = AtomicReference<UInt?>(null)
  private val resumer = AtomicReference<Continuation<Result<String>>>(null)

  fun canLogin(): Boolean = id != null && secret != null

  @MainThread
  suspend fun login(@UiContext context: Context) = suspendCancellableCoroutine<Result<String>> { cont ->
    check(canLogin()) { "GitHub OAuth ID or secret is null" }

    loginRequestState.set(Random(System.nanoTime()).nextUInt())
    resumer.set(cont)

    val url = HttpUrl.Builder()
      .scheme("https")
      .host("github.com")
      .addPathSegments("login/oauth/authorize")
      .addQueryParameter("client_id", id)
      .addQueryParameter("scope", "public_repo")
      .addQueryParameter("state", loginRequestState.get().toString())
      .build()

    context.startActivity(Intent(Intent.ACTION_VIEW, url.toString().toUri()))
  }

  @WorkerThread
  suspend fun resumeWithAccessToken(redirectUri: Uri) {
    Timber.tag(TAG).d("Receive redirect uri: %s", redirectUri)

    val loginRequestState = checkNotNull(loginRequestState.getAndSet(null)) { "Please login() first" }
    val resumer = checkNotNull(resumer.getAndSet(null)) { "Please login() first" }

    val code = redirectUri.getQueryParameter("code")
    val state = redirectUri.getQueryParameter("state")

    if (code == null || state != loginRequestState.toString())
      return resumer.resume(Result.failure(IllegalArgumentException("Invalid uri: $code, $state")))

    resumer.resume(requestAccessToken(code))
  }

  @WorkerThread
  private suspend fun requestAccessToken(code: String): Result<String> {
    val url = HttpUrl.Builder()
      .scheme("https")
      .host("github.com")
      .addPathSegments("login/oauth/access_token")
      .addQueryParameter("client_id", id)
      .addQueryParameter("client_secret", secret)
      .addQueryParameter("code", code)
      .build()
    val request = Request.Builder()
      .url(url)
      .post(EmptyRequest)
      .header("Accept", "application/json")
      .build()

    return runSuspendCatching {
      client.newCall(request).executeAsync().use { response ->
        check(response.isSuccessful) { "Failed to get access token: ${response.code} ${response.message}" }
        checkNotNull(parseAccessToken(response.body.source())) { "Access token is null" }
      }
    }
  }

  private fun parseAccessToken(source: BufferedSource): String? {
    JsonReader.of(source).use { reader ->
      reader.beginObject()
      while (reader.hasNext()) {
        if (reader.nextName() != "access_token") {
          reader.skipValue()
          continue
        }
        return reader.nextString()
      }
      reader.endObject()
    }
    return null
  }

  private data object EmptyRequest : RequestBody() {
    override fun contentType(): MediaType? = null
    override fun writeTo(sink: BufferedSink) {}
  }

  private fun String.orNull(): String? =
    if (isBlank() || this == "null") null else this

  companion object {
    private val TAG = GitHubLogin::class.simpleName!!

    const val LOGIN_URI_SCHEME = "androidx-aosp-viewer"
    const val LOGIN_URI_HOST = "github-login"

    const val LOGOUT_FLAG_DATE = -1L

    // `SimpleDateFormat.getDateInstance()` is not singleton instance
    val LOGIN_DATE_FORMAT: DateFormat = SimpleDateFormat.getDateInstance()
  }
}
