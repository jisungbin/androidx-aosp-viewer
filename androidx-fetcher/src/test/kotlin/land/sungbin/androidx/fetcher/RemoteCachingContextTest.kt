/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import land.sungbin.androidx.fetcher.thirdparty.TaskFaker
import okhttp3.internal.cache.DiskLruCache
import okio.Buffer
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.io.TempDir

class RemoteCachingContextTest {
  @field:TempDir private lateinit var tempDir: Path
  private lateinit var cachingContext: RemoteCachingContext
  private val fs = FakeFileSystem()

  private val taskFaker = TaskFaker()
  private val taskRunner = taskFaker.taskRunner

  private fun createCachingContext() =
    RemoteCachingContext(
      cache = DiskLruCache(
        fileSystem = fs,
        directory = tempDir.toOkioPath(),
        appVersion = RemoteCachingContext.CACHE_VERSION,
        valueCount = RemoteCachingContext.ENTRY_SIZE,
        maxSize = Long.MAX_VALUE,
        taskRunner = taskRunner,
      ),
    )
      .also { context ->
        context.cache!!.initialize()
        cachingContext = context
      }

  @BeforeTest fun prepare() {
    createCachingContext()
  }

  @AfterTest fun cleanup() {
    cachingContext.cache!!.close()
    taskFaker.close()
    fs.checkNoOpenFiles()
  }

  @Test fun cacheMustBeNotNullWhenEnabled() {
    assertFailure { RemoteCachingContext(cache = null, enabled = true) }
      .hasMessage("Cache is enabled but cache is null.")
  }

  @Test fun throwsWhenAccessDisabledCache() {
    assertFailure { RemoteCachingContext(null, false).getCachedSource("") }
      .hasMessage("Cache is null.")

    assertFailure { RemoteCachingContext(null, false).putSource("", Buffer()) }
      .hasMessage("Cache is null.")
  }

  @Test fun uncachedRefReturnsNull() {
    assertThat(cachingContext.getCachedSource(AndroidxRepository.HOME_REF)).isNull()
  }

  @Test fun cachedRefReturnsFilledSource() {
    val testSource = Buffer().writeUtf8("Hello, World!")
    val putResult = cachingContext.putSource(AndroidxRepository.HOME_REF, testSource)

    assertThat(putResult, name = "putResult").isTrue()
    assertThat(cachingContext.getCachedSource(AndroidxRepository.HOME_REF))
      .isNotNull()
      .transform { source -> source.buffer().readUtf8() }
      .isEqualTo("Hello, World!")
  }
}
