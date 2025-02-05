// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.util

inline fun <T : Function<*>> conditionalLambda(
  condition: () -> Boolean,
  lambda: T,
): T? =
  if (condition()) lambda else null
