package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext
import okhttp3.logging.HttpLoggingInterceptor

public data class RemoteLoggingContext(
  public val httpLogging: HttpLoggingInterceptor.Level,
  public val eventLogging: Boolean,
) : CoroutineContext.Element {
  public override val key: CoroutineContext.Key<RemoteLoggingContext> get() = Key

  public companion object Key : CoroutineContext.Key<RemoteLoggingContext>
}