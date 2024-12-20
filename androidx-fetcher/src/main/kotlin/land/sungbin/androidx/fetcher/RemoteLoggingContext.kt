// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext
import okhttp3.logging.HttpLoggingInterceptor

@JvmInline
public value class RemoteLoggingContext(public val level: HttpLoggingInterceptor.Level) : CoroutineContext.Element {
  public override val key: CoroutineContext.Key<RemoteLoggingContext> get() = Key

  public companion object Key : CoroutineContext.Key<RemoteLoggingContext>
}
