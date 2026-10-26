package gov.anzong.androidnga.ui.miuix

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.TwoStatePreference
import com.justwen.androidnga.ui.compose.miuix.MiuixPageContent
import com.justwen.androidnga.ui.compose.miuix.miuixListPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch

/**
 * Renders a live androidx [PreferenceGroup] with Miuix components.
 *
 * The preference objects stay the single source of truth: switches call the preference's own
 * change listener and persist through it, list rows show their entries in a Miuix dropdown and
 * persist through [ListPreference.setValue], and every other row is dispatched through
 * [Preference.performClick], so fragment listeners, dialogs and navigation keep their owners.
 * [revision] must change whenever the hierarchy reports a change so that the state is re-read.
 */
@Composable
internal fun MiuixPreferenceScreen(
    screen: PreferenceGroup?,
    revision: Int,
    page: MiuixPageContent,
    onClick: (Preference) -> Unit,
) {
    val sections = if (screen == null) emptyList() else preferenceSections(screen)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(page.scrollModifier),
        contentPadding = miuixListPadding(page.padding),
    ) {
        items(sections, key = { section -> sectionKey(section) }) { section ->
            Column {
                if (section.title != null) {
                    SmallTitle(text = section.title)
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
                }
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    section.rows.forEach { preference ->
                        PreferenceRow(preference, revision, onClick)
                    }
                }
            }
        }
    }
}

private fun sectionKey(section: PreferenceSection): String =
    section.title ?: ("rows:" + section.rows.joinToString(",") { it.key ?: it.title?.toString().orEmpty() })

@Composable
private fun PreferenceRow(preference: Preference, revision: Int, onClick: (Preference) -> Unit) {
    // Reading revision keeps the row in the recomposition scope that the hierarchy invalidates.
    @Suppress("UNUSED_VARIABLE") val observed = revision
    val title = preference.title?.toString().orEmpty()
    val enabled = preference.isEnabled
    when (preference) {
        is TwoStatePreference -> SuperSwitch(
            title = title,
            summary = preference.summary?.toString()?.takeIf(String::isNotEmpty),
            checked = preference.isChecked,
            enabled = enabled,
            onCheckedChange = { checked ->
                if (preference.callChangeListener(checked)) {
                    preference.isChecked = checked
                }
            },
        )

        is ListPreference -> {
            val entries = preference.entries?.map(CharSequence::toString).orEmpty()
            val values = preference.entryValues?.map(CharSequence::toString).orEmpty()
            val selected = preference.findIndexOfValue(preference.value).coerceAtLeast(0)
            if (entries.isEmpty() || entries.size != values.size || preference.hasCustomDialog()) {
                SuperArrow(
                    title = title,
                    summary = preference.entry?.toString(),
                    enabled = enabled,
                    onClick = { onClick(preference) },
                )
            } else {
                SuperDropdown(
                    title = title,
                    items = entries,
                    selectedIndex = selected,
                    enabled = enabled,
                    onSelectedIndexChange = { index ->
                        val newValue = values[index]
                        if (newValue != preference.value && preference.callChangeListener(newValue)) {
                            preference.value = newValue
                        }
                    },
                )
            }
        }

        else -> SuperArrow(
            title = title,
            summary = preference.summary?.toString()?.takeIf(String::isNotEmpty),
            enabled = enabled,
            onClick = { onClick(preference) },
        )
    }
}

/**
 * List preferences whose fragment overrides the dialog (the image-domain picker needs a text
 * field next to one option) keep their custom flow; they opt in with [EXTRA_CUSTOM_DIALOG].
 */
private fun ListPreference.hasCustomDialog(): Boolean = extras.getBoolean(EXTRA_CUSTOM_DIALOG, false)
