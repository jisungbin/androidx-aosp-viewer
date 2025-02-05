// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.ui.Ui
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesTo(AppScope::class)
interface CircuitComponent {
  val uiFactories: Set<Ui.Factory>

  @SingleIn(AppScope::class)
  @Provides fun provideCircuit(uiFactories: Set<Ui.Factory>): Circuit =
    Circuit.Builder()
      .addUiFactories(uiFactories)
      .build()

  val circuit: Circuit
}
