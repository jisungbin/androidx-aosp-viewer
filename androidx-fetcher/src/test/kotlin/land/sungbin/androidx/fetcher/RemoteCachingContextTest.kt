// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import okhttp3.internal.cache.DiskLruCache
import okio.Buffer
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.io.TempDir
import thirdparty.TaskFaker

class RemoteCachingContextTest {
  @field:TempDir private lateinit var tempDir: Path
  private var cache: AndroidxRepositoryCache? = null
  private val fs = FakeFileSystem()

  private val taskFaker = TaskFaker()
  private val testTaskRunner = taskFaker.taskRunner

  @BeforeTest fun prepare() {
    cache = AndroidxRepositoryCache(
      DiskLruCache(
        fileSystem = fs,
        directory = tempDir.toOkioPath(),
        appVersion = AndroidxRepositoryCache.CACHE_VERSION,
        valueCount = AndroidxRepositoryCache.ENTRY_SIZE,
        maxSize = Long.MAX_VALUE,
        taskRunner = testTaskRunner,
      ),
    )
  }

  @AfterTest fun cleanup() {
    cache!!.cache.close()
    taskFaker.close()
    fs.checkNoOpenFiles()
  }

  @Test fun uncachedRefReturnsNull() {
    assertThat(cache!!.getCachedSource(AndroidxRepository.HOME_REF)).isNull()
  }

  @Test fun cachedRefReturnsFilledSource() {
    val cache = cache!!

    val testSource = Buffer().writeUtf8("Hello, World!")
    val putResult = cache.putSource(AndroidxRepository.HOME_REF, testSource.readByteString())

    assertThat(putResult, name = "write cache").isTrue()
    assertThat(cache.getCachedSource(AndroidxRepository.HOME_REF), name = "read cache")
      .isNotNull()
      .transform { source -> source.buffer().readUtf8() }
      .isEqualTo("Hello, World!")
  }
}
