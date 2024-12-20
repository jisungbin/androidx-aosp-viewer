// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import land.sungbin.androidx.viewer.GitHubLogin
import land.sungbin.androidx.viewer.R
import land.sungbin.androidx.viewer.preferences.PreferencesKey

private val notNumberRegex = Regex("\\D")

// Used in ModalBottomSheet.content
@Composable fun ColumnScope.GHSettingSheetContent(
  modifier: Modifier = Modifier,
  dataStore: DataStore<Preferences>,
  onLoginToggleClick: () -> Unit,
) {
  val ioScope = rememberCoroutineScope(Dispatchers::IO)

  val fontSize by dataStore.data
    .map { it[PreferencesKey.WithDefault.fontSize.key] ?: PreferencesKey.WithDefault.fontSize.default }
    .collectAsStateWithLifecycle(PreferencesKey.WithDefault.fontSize.default)
  var maxCacheSize by remember { mutableLongStateOf(PreferencesKey.WithDefault.maxCacheSize.default) }
  var ghLoginDate by remember { mutableLongStateOf(GitHubLogin.LOGOUT_FLAG) }

  DisposableEffect(dataStore) {
    ioScope.launch {
      dataStore.data.collect { preferences ->
        val newMaxCacheSize = preferences[PreferencesKey.WithDefault.maxCacheSize.key]
          ?: PreferencesKey.WithDefault.maxCacheSize.default
        val newGhLoginDate = preferences[PreferencesKey.ghLoginDate] ?: GitHubLogin.LOGOUT_FLAG

        maxCacheSize = newMaxCacheSize
        ghLoginDate = newGhLoginDate
      }
    }

    onDispose {
      ioScope.launch {
        dataStore.edit { preferences ->
          preferences[PreferencesKey.WithDefault.maxCacheSize.key] = maxCacheSize.coerceAtLeast(0L)

          // - The ghLoginDate data is managed separately.
          // - The fontSize data is saved immediately because it needs to be applied in real-time.
        }
      }
    }
  }

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
        onClick = {
          ioScope.launch {
            dataStore.edit { preferences ->
              preferences[PreferencesKey.WithDefault.fontSize.key] = fontSize + 1
            }
          }
        },
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_round_add_24),
          contentDescription = "Increase font size",
        )
      }
      Text(
        modifier = Modifier.padding(horizontal = 5.dp),
        text = fontSize.toString(),
        style = MaterialTheme.typography.bodyMedium,
      )
      IconButton(
        enabled = fontSize - 1 >= 10,
        onClick = {
          ioScope.launch {
            dataStore.edit { preferences ->
              preferences[PreferencesKey.WithDefault.fontSize.key] = fontSize - 1
            }
          }
        },
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_round_remove_24),
          contentDescription = "Decrease font size",
        )
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
          modifier = Modifier.paddingFromBaseline(top = 15.dp),
          text = stringResource(R.string.preferences_cache_disable_hint),
          style = MaterialTheme.typography.labelSmall,
        )
      }
      // TODO number formatting visualTransformation
      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(0.7f),
        value = maxCacheSize.coerceAtLeast(0L).toString(),
        onValueChange = { maxCacheSize = it.replace(notNumberRegex, "").toLongOrNull() ?: -1 },
        textStyle = MaterialTheme.typography.bodyMedium,
        suffix = { Text(" " + stringResource(R.string.preferences_cache_size_unit)) },
        keyboardOptions = remember {
          KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
          )
        },
        singleLine = true,
      )
    }
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.preferences_gh_login), style = MaterialTheme.typography.bodyMedium)
      Button(
        modifier = Modifier
          .padding(top = 10.dp)
          .fillMaxWidth(),
        onClick = onLoginToggleClick,
      ) {
        Text(
          text = run {
            if (ghLoginDate == GitHubLogin.LOGOUT_FLAG) stringResource(R.string.preferences_gh_logged_out)
            else stringResource(R.string.preferences_gh_logged_in, ghLoginDate.toDateString())
          },
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

private fun Long.toDateString(): String = GitHubLogin.LOGIN_DATE_FORMAT.format(Date(this))
