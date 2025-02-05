// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import land.sungbin.androidx.fetcher.AndroidxRepositoryReader
import land.sungbin.androidx.viewer.presenter.CodeScreenPresenter
import land.sungbin.androidx.viewer.presenter.SettingScreenPresenter
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesTo(AppScope::class)
interface PresenterComponent {
  val codeScreenPresenter: (AndroidxRepositoryReader) -> CodeScreenPresenter
  val settingScreenPresenter: (DataStore<Preferences>) -> SettingScreenPresenter
}
