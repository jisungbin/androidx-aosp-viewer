// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

@Composable fun GHContentLoading(modifier: Modifier = Modifier) {
  DotLottieAnimation(
    modifier = modifier,
    source = DotLottieSource.Asset("whale.lottie"),
    autoplay = true,
    loop = true,
  )
}
