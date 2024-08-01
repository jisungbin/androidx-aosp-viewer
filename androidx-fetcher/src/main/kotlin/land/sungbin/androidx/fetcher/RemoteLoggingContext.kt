/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext
import okhttp3.logging.HttpLoggingInterceptor

@JvmInline
public value class RemoteLoggingContext(public val level: HttpLoggingInterceptor.Level) : CoroutineContext.Element {
  public override val key: CoroutineContext.Key<RemoteLoggingContext> get() = Key

  public companion object Key : CoroutineContext.Key<RemoteLoggingContext>
}
