// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext

@JvmInline
public value class GitHubAuthorizationContext(public val token: String?) : CoroutineContext.Element {
  public override val key: CoroutineContext.Key<GitHubAuthorizationContext> get() = Key

  public companion object Key : CoroutineContext.Key<GitHubAuthorizationContext>
}
