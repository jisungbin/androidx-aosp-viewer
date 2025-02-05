// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.util

import androidx.annotation.StringRes

fun interface StringResolver {
  fun getString(@StringRes resId: Int): String
}
