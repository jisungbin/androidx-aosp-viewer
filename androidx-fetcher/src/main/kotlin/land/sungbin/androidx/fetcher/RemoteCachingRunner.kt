// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import okhttp3.internal.concurrent.TaskRunner
import okhttp3.internal.concurrent.TaskRunner.RealBackend

public object RemoteCachingRunner {
  private val backend: TaskRunner.Backend by lazy {
    RealBackend { runnable ->
      Thread(runnable, RemoteCachingRunner::class.simpleName!!)
        .apply { isDaemon = true }
    }
  }

  public operator fun invoke(): TaskRunner = TaskRunner(backend)
}
