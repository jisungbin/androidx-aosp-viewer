package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext
import okio.FileSystem
import okio.Path

public data class RemoteCachingContext(
  public val fs: FileSystem,
  public val directory: Path,
  public val maxSize: Long,
) : CoroutineContext.Element {
  override val key: CoroutineContext.Key<RemoteCachingContext> get() = Key

  public companion object Key : CoroutineContext.Key<RemoteCachingContext>
}