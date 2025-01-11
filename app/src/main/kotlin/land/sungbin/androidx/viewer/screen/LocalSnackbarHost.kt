// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHost: ProvidableCompositionLocal<SnackbarHostState> =
  staticCompositionLocalOf { error("No SnackbarHost provided") }
