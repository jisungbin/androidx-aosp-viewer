/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import java.util.concurrent.ThreadFactory
import okhttp3.internal.concurrent.TaskRunner
import okhttp3.internal.concurrent.TaskRunner.RealBackend

public object RemoteCachingRunner {
  private val backend by lazy {
    RealBackend(
      ThreadFactory { runnable ->
        Thread(runnable, RemoteCachingRunner::class.simpleName!!)
          .apply { isDaemon = true }
      },
    )
  }

  public operator fun invoke(): TaskRunner = TaskRunner(backend)
}
