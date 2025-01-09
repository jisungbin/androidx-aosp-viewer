// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.design

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable fun GHContentLoading(modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}
