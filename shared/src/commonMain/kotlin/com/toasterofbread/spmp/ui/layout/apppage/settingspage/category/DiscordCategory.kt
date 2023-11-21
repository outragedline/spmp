package com.toasterofbread.spmp.ui.layout.apppage.settingspage.category

import LocalPlayerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.settings.ui.item.BasicSettingsValueState
import com.toasterofbread.composekit.settings.ui.item.SettingsComposableItem
import com.toasterofbread.composekit.settings.ui.item.SettingsGroupItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItem
import com.toasterofbread.composekit.settings.ui.item.SettingsItemInfoText
import com.toasterofbread.composekit.settings.ui.item.SettingsLargeToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsTextFieldItem
import com.toasterofbread.composekit.settings.ui.item.SettingsToggleItem
import com.toasterofbread.composekit.settings.ui.item.SettingsValueState
import com.toasterofbread.composekit.utils.composable.LinkifyText
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spmp.model.settings.Settings
import com.toasterofbread.spmp.model.settings.category.AuthSettings
import com.toasterofbread.spmp.model.settings.category.DiscordSettings
import com.toasterofbread.spmp.model.settings.category.InternalSettings
import com.toasterofbread.spmp.platform.DiscordStatus
import com.toasterofbread.spmp.resources.getString
import com.toasterofbread.spmp.ui.layout.DiscordAccountPreview
import com.toasterofbread.spmp.ui.layout.DiscordLoginConfirmation
import com.toasterofbread.spmp.ui.layout.apppage.settingspage.PrefsPageScreen

internal fun getDiscordCategoryItems(): List<SettingsItem> {
    if (!DiscordStatus.isSupported()) {
        return emptyList()
    }

    val discord_auth: SettingsValueState<String> = SettingsValueState<String>(
        AuthSettings.Key.DISCORD_ACCOUNT_TOKEN.getName()
    ).init(Settings.prefs, Settings::provideDefault)
    var account_token by mutableStateOf(discord_auth.get())

    return listOf(
        SettingsComposableItem {
            var accepted: Boolean by InternalSettings.Key.DISCORD_WARNING_ACCEPTED.rememberMutableState()

            AnimatedVisibility(!accepted, enter = expandVertically(), exit = shrinkVertically()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = background,
                        contentColor = on_background
                    ),
                    border = BorderStroke(2.dp, Color.Red),
                ) {
                    Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red)

                        LinkifyText(getString("warning_discord_kizzy"), accent, colour = on_background, style = MaterialTheme.typography.bodyMedium)

                        Button(
                            { accepted = true },
                            Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = on_accent
                            )
                        ) {
                            Text(
                                getString("action_warning_accept")
                            )
                        }
                    }
                }
            }
        },

        SettingsLargeToggleItem(
            object : BasicSettingsValueState<Boolean> {
                override fun getKeys(): List<String> = discord_auth.getKeys()
                override fun get(): Boolean = discord_auth.get().isNotEmpty()
                override fun set(value: Boolean) {
                    if (!value) {
                        discord_auth.set("")
                    }
                }

                override fun init(prefs: PlatformPreferences, defaultProvider: (String) -> Any): BasicSettingsValueState<Boolean> = this
                override fun release(prefs: PlatformPreferences) {}
                override fun setEnableAutosave(value: Boolean) {}
                override fun reset() = discord_auth.reset()
                override fun PlatformPreferences.Editor.save() = with (discord_auth) { save() }
                override fun getDefault(defaultProvider: (String) -> Any): Boolean =
                    (defaultProvider(AuthSettings.Key.DISCORD_ACCOUNT_TOKEN.getName()) as String).isNotEmpty()

                @Composable
                override fun onChanged(key: Any?, action: (Boolean) -> Unit) {
                    TODO()
                }
            },
            enabledContent = { modifier ->
                val auth = discord_auth.get()
                if (auth.isNotEmpty()) {
                    account_token = auth
                }
                if (account_token.isNotEmpty()) {
                    DiscordAccountPreview(account_token, modifier)
                }
            },
            disabled_text = getString("auth_not_signed_in"),
            enable_button = getString("auth_sign_in"),
            disable_button = getString("auth_sign_out"),
            warningDialog = { dismiss, openPage ->
                DiscordLoginConfirmation { manual ->
                    dismiss()
                    if (manual != null) {
                        openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal, manual)
                    }
                }
            },
            infoButton = { enabled, _ ->
                val player = LocalPlayerState.current
                var show_info_dialog: Boolean by remember { mutableStateOf(false) }

                if (show_info_dialog) {
                    DiscordLoginConfirmation(true) {
                        show_info_dialog = false
                    }
                }

                ShapedIconButton(
                    { show_info_dialog = !show_info_dialog },
                    shape = CircleShape,
                    colours = IconButtonDefaults.iconButtonColors(
                        containerColor = if (enabled) player.theme.background else player.theme.vibrant_accent,
                        contentColor = if (enabled) player.theme.on_background else player.theme.on_accent
                    )
                ) {
                    Icon(
                        if (enabled) Icons.Default.Settings
                        else Icons.Default.Info,
                        null
                    )
                }
            },
            prerequisite_value = SettingsValueState(InternalSettings.Key.DISCORD_WARNING_ACCEPTED.getName())
        ) { target, setEnabled, _, openPage ->
            if (target) {
                openPage(PrefsPageScreen.DISCORD_LOGIN.ordinal, null)
            }
            else {
                setEnabled(false)
            }
        },

        SettingsGroupItem(getString("s_group_discord_status_disable_when")),

        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_INVISIBLE.getName()),
            getString("s_key_discord_status_disable_when_invisible"), null
        ),
        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_DND.getName()),
            getString("s_key_discord_status_disable_when_dnd"), null
        ),
        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_IDLE.getName()),
            getString("s_key_discord_status_disable_when_idle"), null
        ),
        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_OFFLINE.getName()),
            getString("s_key_discord_status_disable_when_offline"), null
        ),
        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.STATUS_DISABLE_WHEN_ONLINE.getName()),
            getString("s_key_discord_status_disable_when_online"), null
        ),

        SettingsGroupItem(getString("s_group_discord_status_content")),

        SettingsItemInfoText(getString("s_discord_status_text_info")),

        SettingsTextFieldItem(
            SettingsValueState(DiscordSettings.Key.STATUS_NAME.getName()),
            getString("s_key_discord_status_name"), getString("s_sub_discord_status_name")
        ),
        SettingsTextFieldItem(
            SettingsValueState(DiscordSettings.Key.STATUS_TEXT_A.getName()),
            getString("s_key_discord_status_text_a"), getString("s_sub_discord_status_text_a")
        ),
        SettingsTextFieldItem(
            SettingsValueState(DiscordSettings.Key.STATUS_TEXT_B.getName()),
            getString("s_key_discord_status_text_b"), getString("s_sub_discord_status_text_b")
        ),
        SettingsTextFieldItem(
            SettingsValueState(DiscordSettings.Key.STATUS_TEXT_C.getName()),
            getString("s_key_discord_status_text_c"), getString("s_sub_discord_status_text_c")
        ),

        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.SHOW_SONG_BUTTON.getName()),
            getString("s_key_discord_status_show_button_song"), getString("s_sub_discord_status_show_button_song")
        ),
        SettingsTextFieldItem(
            SettingsValueState(DiscordSettings.Key.SONG_BUTTON_TEXT.getName()),
            getString("s_key_discord_status_button_song_text"), null
        ),
        SettingsToggleItem(
            SettingsValueState(DiscordSettings.Key.SHOW_PROJECT_BUTTON.getName()),
            getString("s_key_discord_status_show_button_project"), getString("s_sub_discord_status_show_button_project")
        ),
        SettingsTextFieldItem(
            SettingsValueState(DiscordSettings.Key.PROJECT_BUTTON_TEXT.getName()),
            getString("s_key_discord_status_button_project_text"), null
        )
    )
}