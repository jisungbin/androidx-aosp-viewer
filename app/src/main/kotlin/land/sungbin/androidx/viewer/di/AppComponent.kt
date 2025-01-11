// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.di

import land.sungbin.androidx.viewer.GitHubLogin
import land.sungbin.androidx.viewer.presenter.CodeScreenPresenter
import land.sungbin.androidx.viewer.screen.CodeScreen
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent {
  abstract val ghLogin: GitHubLogin

  abstract val codeScreenPresenter: CodeScreenPresenter
  abstract val codeScreenSharedState: CodeScreen.SharedState
}
