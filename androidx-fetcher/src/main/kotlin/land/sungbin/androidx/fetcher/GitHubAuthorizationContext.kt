/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext

@JvmInline
public value class GitHubAuthorizationContext(public val token: String?) : CoroutineContext.Element {
  public override val key: CoroutineContext.Key<GitHubAuthorizationContext> get() = Key

  public companion object Key : CoroutineContext.Key<GitHubAuthorizationContext>
}
