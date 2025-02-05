// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.screen

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.drewhamilton.poko.Poko
import java.util.Date
import kotlinx.parcelize.Parcelize
import land.sungbin.androidx.viewer.GitHubLogin
import land.sungbin.androidx.viewer.R
import land.sungbin.androidx.viewer.screen.SettingScreen.Event.ToggleGitHubLogin
import land.sungbin.androidx.viewer.screen.SettingScreen.Event.UpdatePreferences
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

@Parcelize data object SettingScreen : Screen {
  @Immutable data class State(
    val fontSize: Int,
    val maxCacheSize: Long,
    val ghLoginDate: Long,
    val eventSink: (Event) -> Unit,
  ) : CircuitUiState

  sealed interface Event : CircuitUiEvent {
    @Poko class UpdatePreferences(
      val fontSize: Int? = null,
      val maxCacheSize: Long? = null,
    ) : Event

    @Poko class ToggleGitHubLogin(val windowHost: Activity) : Event
  }
}

private val notNumberRegex = Regex("\\D")

@CircuitInject(SettingScreen::class, AppScope::class)
@Composable fun Settings(state: SettingScreen.State, modifier: Modifier = Modifier) {
  val activity = LocalActivity.current!!

  val fontSize by rememberUpdatedState(state.fontSize)
  val maxCacheSize by rememberUpdatedState(state.maxCacheSize)
  val ghLoginDate by rememberUpdatedState(state.ghLoginDate)

  Column(
    modifier = modifier.padding(all = 20.dp),
    verticalArrangement = Arrangement.spacedBy(15.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(stringResource(R.string.preferences_font_size), style = MaterialTheme.typography.bodyMedium)
      Spacer(Modifier.weight(1f))
      IconButton(
        enabled = fontSize + 1 <= 50,
        onClick = { state.eventSink(UpdatePreferences(fontSize = fontSize + 1)) },
      ) {
        Icon(painterResource(R.drawable.ic_fill_add_24), "Increase font size")
      }
      Text(
        fontSize.toString(),
        modifier = Modifier.padding(horizontal = 5.dp),
        style = MaterialTheme.typography.bodyMedium,
      )
      IconButton(
        enabled = fontSize - 1 >= 10,
        onClick = { state.eventSink(UpdatePreferences(fontSize = fontSize - 1)) },
      ) {
        Icon(painterResource(R.drawable.ic_fill_remove_24), "Decrease font size")
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(stringResource(R.string.preferences_max_cache_size), style = MaterialTheme.typography.bodyMedium)
        Text(
          stringResource(R.string.preferences_cache_disable_hint),
          modifier = Modifier.paddingFromBaseline(top = 15.dp),
          style = MaterialTheme.typography.labelSmall,
        )
      }
      // TODO number formatting via VisualTransformation
      OutlinedTextField(
        value = maxCacheSize.coerceAtLeast(0L).toString(),
        onValueChange = { newMaxCacheSize ->
          val preferenceValue = newMaxCacheSize.replace(notNumberRegex, "").toLongOrNull() ?: -1
          state.eventSink(UpdatePreferences(maxCacheSize = preferenceValue))
        },
        modifier = Modifier.fillMaxWidth(0.7f),
        textStyle = MaterialTheme.typography.bodyMedium,
        suffix = { Text(" " + stringResource(R.string.preferences_cache_size_unit)) },
        keyboardOptions = remember { KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done) },
        singleLine = true,
      )
    }
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.preferences_gh_login), style = MaterialTheme.typography.bodyMedium)
      Button(
        modifier = Modifier
          .padding(top = 10.dp)
          .fillMaxWidth(),
        onClick = { state.eventSink(ToggleGitHubLogin(windowHost = activity)) },
      ) {
        Text(
          if (ghLoginDate == GitHubLogin.LOGOUT_FLAG_DATE)
            stringResource(R.string.preferences_gh_logged_out)
          else
            stringResource(R.string.preferences_gh_logged_in, ghLoginDate.toDateString()),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      Text(
        stringResource(R.string.preferences_gh_login_app_restart),
        modifier = Modifier
          .padding(top = 5.dp)
          .align(Alignment.CenterHorizontally),
        style = MaterialTheme.typography.labelSmall,
      )
    }
  }
}

private fun Long.toDateString(): String = GitHubLogin.LOGIN_DATE_FORMAT.format(Date(this))
