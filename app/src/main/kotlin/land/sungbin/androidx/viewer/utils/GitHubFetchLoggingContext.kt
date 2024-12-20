// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.utils

import land.sungbin.androidx.fetcher.RemoteLoggingContext
import okhttp3.logging.HttpLoggingInterceptor

val GitHubFetchLoggingContext = RemoteLoggingContext(level = HttpLoggingInterceptor.Level.BODY)
