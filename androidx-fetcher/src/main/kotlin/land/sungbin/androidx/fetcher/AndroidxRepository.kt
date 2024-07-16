package land.sungbin.androidx.fetcher

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

public class AndroidxRepository(
  private val base: HttpUrl = "https://api.github.com".toHttpUrl(),
  private val logger: Logger = Logger.Default,
  private val dispatcher: CoroutineContext = Dispatchers.IO,
)