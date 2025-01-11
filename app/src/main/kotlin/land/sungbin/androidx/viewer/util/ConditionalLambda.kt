// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.util

inline fun <T> conditionalLambda(
  condition: () -> Boolean,
  noinline block: () -> T,
): (() -> T)? =
  if (condition()) block else null
