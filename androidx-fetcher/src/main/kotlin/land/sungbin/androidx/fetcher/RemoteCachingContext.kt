/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext
import okio.FileSystem
import okio.Path

public data class RemoteCachingContext(
  public val fs: FileSystem,
  public val directory: Path,
  public val maxSize: Long, // bytes
) : CoroutineContext.Element {
  override val key: CoroutineContext.Key<RemoteCachingContext> get() = Key

  public companion object Key : CoroutineContext.Key<RemoteCachingContext>
}
