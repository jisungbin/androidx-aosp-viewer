/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer.utils

import land.sungbin.androidx.fetcher.RemoteLoggingContext
import okhttp3.logging.HttpLoggingInterceptor

val GitHubFetchLoggingContext =
  RemoteLoggingContext(
    httpLogging = HttpLoggingInterceptor.Level.BODY,
    eventLogging = false,
  )
