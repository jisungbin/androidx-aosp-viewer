package land.sungbin.androidx.fetcher

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryTest {
  private lateinit var logger: TestLogger
  private lateinit var server: MockWebServer
  private lateinit var repo: AndroidxRepository

  @BeforeTest fun prepare(server: MockWebServer) {
    logger = TestLogger()
    this.server = server
    repo = AndroidxRepository(server.url("/"), logger, UnconfinedTestDispatcher())
  }

  @Test fun given_remoteLoggingContext_with_level_above_none_when_fetching_enable_http_logging() {

  }

  @Test fun given_remoteLoggingContext_with_level_none_when_fetching_disable_http_logging() {

  }

  @Test fun given_remoteLoggingContext_with_event_logging_when_fetching_enable_event_logging() {

  }

  @Test fun given_remoteLoggingContext_with_both_logs_enabled_when_fetching_enable_http_and_event_logging() {

  }

  @Test fun given_remoteCachingContext_when_fetching_apply_http_cache() {

  }

  @Test fun given_githubAuthorizationContext_when_fetching_add_authorization_header() {

  }

  @Test fun given_api_response_is_not_successful_when_fetching_log_error() {

  }
}