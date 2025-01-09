package land.sungbin.androidx.viewer.component

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import me.tatarka.inject.annotations.Provides
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@MergeComponent(AppScope::class)
@SingleIn(AppScope::class)
abstract class AppComponent {
  // abstract val presenterFactories: Set<Presenter.Factory>
  abstract val uiFactories: Set<Ui.Factory>

  @SingleIn(AppScope::class)
  @Provides
  fun providerCircuitBuilder(
    // presenterFactories: Set<Presenter.Factory>,
    uiFactories: Set<Ui.Factory>,
  ): Circuit.Builder =
    Circuit.Builder()
      // .addPresenterFactories(presenterFactories)
      .addUiFactories(uiFactories)

  abstract val circuitBuilder: Circuit.Builder
}
