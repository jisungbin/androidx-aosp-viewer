// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import java.nio.file.Path
import kotlin.test.Test
import okhttp3.internal.cache.DiskLruCache
import okio.Buffer
import okio.Closeable
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.io.TempDir
import thirdparty.TaskFaker

class AndroidxRepositoryCacheTest {
  @Test fun createsInstance(@TempDir path: Path) {
    cache(path)
  }

  @Test fun initializesCacheOnCreation(@TempDir path: Path) {
    val cache = cache(path)
    val cacheIsInitialized = DiskLruCache::class.java
      .getDeclaredField("initialized")
      .apply { isAccessible = true }
      .get(cache.cache) as Boolean

    assertThat(cacheIsInitialized).isTrue()
  }

  @Test fun newCachesWithNewKey(@TempDir path: Path) {
    val cache = cache(path)
    val result = cache.putSource("key", bufferOf("value"))

    assertThat(result, name = "caching result").isTrue()
    cache.assertCachedValue("key", "value")
  }

  @Test fun overwritesCacheWithDuplicateKey(@TempDir path: Path) {
    val cache = cache(path)
    var result = cache.putSource("key", bufferOf("value"))

    assertThat(result, name = "caching").isTrue()
    cache.assertCachedValue("key", "value", name = "new")

    result = cache.putSource("key", bufferOf("duplicated long value"))

    assertThat(result, name = "duplicated caching").isTrue()
    cache.assertCachedValue("key", "duplicated long value", name = "long duplicated")

    result = cache.putSource("key", bufferOf("short value"))

    assertThat(result, name = "duplicated caching 2").isTrue()
    cache.assertCachedValue("key", "short value", name = "short duplicated")
  }

  // Although it is closer to code that tests DiskLruCache itself (assuming that the library
  // provider has sufficiently tested it), the purpose of this test is to verify whether I
  // have used the DiskLruCache API correctly.
  @Test fun cachesLargeSource(@TempDir path: Path) {
    val cache = cache(path)
    val result = cache.putSource("key", bufferOf(LONG_CONTENT))

    assertThat(result, name = "caching").isTrue()
    cache.assertCachedValue("key", LONG_CONTENT)
  }

  @Test fun returnsNullWhenNoCachedSource(@TempDir path: Path) {
    val cache = cache(path)

    assertThat(cache.getCachedSource("key")).isNull()
  }

  @Test fun doesNotCloseSourceAfterCaching(@TempDir path: Path) {
    val cache = cache(path)
    val source = bufferOf("content")
    val result = cache.putSource("key", source)

    assertThat(result, name = "caching").isTrue()
    assertThat(source.exhausted(), name = "exhausted").isFalse()
    assertThat(source.readUtf8()).isEqualTo("content")
  }

  @Test fun doesNotCacheEmptySource(@TempDir path: Path) {
    val cache = cache(path)
    val source = Buffer()
    val result = cache.putSource("key", source)

    assertThat(result, name = "caching").isFalse()
    assertThat(cache.getCachedSource("key")).isNull()
  }

  @Test fun evictsAllCache(@TempDir path: Path) {
    val cache = cache(path)
    val result = cache.putSource("key", bufferOf("value"))
    val result2 = cache.putSource("key2", bufferOf("value2"))

    assertThat(result, name = "caching").isTrue()
    assertThat(result2, name = "caching 2").isTrue()

    assertThat(cache.evictAll(), name = "evicting").isTrue()
    assertThat(cache.getCachedSource("key")).isNull()
    assertThat(cache.getCachedSource("key2")).isNull()
  }

  private fun AndroidxRepositoryCache.assertCachedValue(key: String, expect: String, name: String? = null) {
    assertThat(getCachedSource(key), name = name)
      .isNotNull()
      .transform { it.use { source -> source.buffer().readUtf8() } }
      .isEqualTo(expect)
  }

  private fun cache(path: Path): AndroidxRepositoryCache {
    val fs = FakeFileSystem().also(toCloses::add)
    val taskFaker = TaskFaker().also(toCloses::add)

    return AndroidxRepositoryCache(
      DiskLruCache(
        fileSystem = fs,
        directory = path.toOkioPath(),
        appVersion = 1,
        valueCount = AndroidxRepositoryCache.ENTRY_SIZE,
        maxSize = Long.MAX_VALUE,
        taskRunner = taskFaker.taskRunner,
      ),
    )
      .also(toCloses::add)
  }

  private companion object {
    private val LONG_CONTENT = """
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Facillime numquidnam, 
veterum d antiquis. Nostris vivi, tractatos, imperitorum graecum, partem terentii.
Latinis optinere desideraret amarissimam quadam prospexit. Exercitationem interea 
docendi paria afranius eoque oderit. Miseram difficilem torquate exercendi tristique 
interdum, confirmatur putavisset patrius quaeritur. Numquid crudelis sensuum quosvis 
debitis stoicis quodsi contrariis gravitate huius, mediocriterne. Romanum accedere 
magnitudinem posteri inhaererent captiosa intelleges odit temperantia melius. Splendide 
sanguinem caecilii abducat tamen quosvis metum animadversionem interea, suo. Reprehenderit 
neglegentur, amaret crudelis, eos, petat eveniunt conubia. Aperiri stabilique consuetudinum 
aliquod vacuitate, putet expedire, fore nominata culpa modis. Vocent fore distinguique
audivi infimum appellantur perpetuam consumere dissident. Loca ordinem oculis oderit 
stoicis suavitate didicisse legere expediunt maximi suspicio. Amarissimam forensibus 
magni omnium opinemur maecenas hominum corporum. Ei perpetua porttitor electram sensum. 
Intemperantes asperum afferat theophrasti, infantes isti videretur tamen platonis. Sensibus
dolorem declinationem principio arguerent facerem. Aliquando voluptaria quietus responsum
nasci testibus diceret dicebas, terminari totus pleniorem. Laetamur legerint terrore 
exorsus ardore consedit. Malo omnino, exorsus, hanc coniunctione etsi. Siculis reprimiqu
silano lictores physico, philosophari animadvertat sic exorsus. Certe patet scriptorem 
transferre quietus iudicante.
    """.repeat(10)

    private val toCloses = mutableListOf<Closeable>()

    @JvmStatic
    @AfterAll fun tearDown() {
      toCloses.forEach(Closeable::close)
      toCloses.clear()
    }
  }
}

fun bufferOf(content: String) = Buffer().apply { writeUtf8(content) }
