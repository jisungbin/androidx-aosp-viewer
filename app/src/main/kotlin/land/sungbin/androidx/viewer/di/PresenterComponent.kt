// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.di

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.slack.circuit.runtime.presenter.Presenter
import land.sungbin.androidx.viewer.presenter.CodeScreenPresenter
import land.sungbin.androidx.viewer.presenter.SettingScreenPresenter
import land.sungbin.androidx.viewer.screen.CodeScreen
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesTo(AppScope::class)
interface PresenterComponent {
  val settingScreenPresenter: (dataStore: DataStore<Preferences>, host: Activity) -> SettingScreenPresenter

  @SingleIn(AppScope::class)
  @IntoSet
  @Provides fun provideCodeScreenPresenterFactory(presenter: CodeScreenPresenter): Presenter.Factory =
    Presenter.Factory { screen, _, _ ->
      if (screen == CodeScreen) presenter else null
    }
}
