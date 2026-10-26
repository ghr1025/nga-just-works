package gov.anzong.androidnga.ui.miuix

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup

/** One HyperOS settings section: an optional [SmallTitle] followed by a [Card] of rows. */
internal class PreferenceSection(val title: String?, val rows: List<Preference>)

/**
 * Groups the visible preferences of [screen] the way a HyperOS settings page does: every
 * [PreferenceCategory] becomes a titled card, and consecutive top-level rows share one card.
 */
internal fun preferenceSections(screen: PreferenceGroup): List<PreferenceSection> {
    val sections = ArrayList<PreferenceSection>()
    var loose = ArrayList<Preference>()
    fun flushLoose() {
        if (loose.isNotEmpty()) {
            sections += PreferenceSection(null, loose)
            loose = ArrayList()
        }
    }
    for (index in 0 until screen.preferenceCount) {
        val preference = screen.getPreference(index)
        if (!preference.isVisible) continue
        if (preference is PreferenceCategory) {
            flushLoose()
            val rows = (0 until preference.preferenceCount)
                .map(preference::getPreference)
                .filter(Preference::isVisible)
            if (rows.isNotEmpty()) {
                sections += PreferenceSection(preference.title?.toString(), rows)
            }
        } else {
            loose += preference
        }
    }
    flushLoose()
    return sections
}
