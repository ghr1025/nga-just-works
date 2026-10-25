package gov.anzong.androidnga.ui.miuix

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.justwen.androidnga.ui.compose.miuix.MiuixHostMenuActions
import com.justwen.androidnga.ui.compose.miuix.MiuixNavIcon
import com.justwen.androidnga.ui.compose.miuix.MiuixPage
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeOwner
import com.justwen.androidnga.ui.compose.miuix.NgaMiuixTheme
import com.justwen.androidnga.ui.compose.miuix.rememberHostOptionsMenu

/**
 * Wraps the view of a [PreferenceFragmentCompat] in a Miuix page.
 *
 * The fragment keeps its RecyclerView and PreferenceGroupAdapter (hidden): the adapter is what
 * PreferenceFragmentCompat binds to the hierarchy, so its change notifications are forwarded
 * to [revision] and recompose the Miuix rows. Clicks go through [Preference.performClick],
 * which the fragment's own onPreferenceTreeClick / dialog handling still receives.
 */
internal class MiuixPreferenceHost(private val fragment: PreferenceFragmentCompat) {

    private var revision by mutableIntStateOf(0)

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() = bump()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = bump()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = bump()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = bump()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = bump()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = bump()
    }

    private var observedAdapter: RecyclerView.Adapter<*>? = null

    private fun bump() {
        revision++
    }

    /** Returns the root to use instead of the classic preference list. */
    fun wrap(classicView: View): View {
        val context = fragment.requireContext()
        classicView.visibility = View.GONE
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaMiuixTheme {
                    val activity = fragment.activity
                    val chrome = (activity as? MiuixChromeOwner)?.miuixChrome
                    val menu = rememberHostOptionsMenu(activity)
                    MiuixPage(
                        title = chrome?.title.orEmpty(),
                        onNavigationClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
                        navIcon = MiuixNavIcon.Back,
                        actions = { MiuixHostMenuActions(activity, menu) },
                    ) { page ->
                        MiuixPreferenceScreen(
                            screen = fragment.preferenceScreen,
                            revision = revision,
                            page = page,
                            onClick = Preference::performClickCompat,
                        )
                    }
                }
            }
        }
        return FrameLayout(context).apply {
            addView(classicView, ViewGroup.LayoutParams(0, 0))
            addView(
                compose,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
    }

    /** Call from onViewCreated, after PreferenceFragmentCompat has bound its adapter. */
    fun attach() {
        fragment.listView?.post {
            val adapter = fragment.listView?.adapter ?: return@post
            if (adapter !== observedAdapter) {
                observedAdapter?.unregisterAdapterDataObserver(observer)
                adapter.registerAdapterDataObserver(observer)
                observedAdapter = adapter
            }
            bump()
        }
    }

    /** Re-reads every row, e.g. when the fragment changed preference state directly. */
    fun refresh() = bump()

    fun detach() {
        observedAdapter?.unregisterAdapterDataObserver(observer)
        observedAdapter = null
    }
}

/**
 * [Preference.performClick] is the public-in-practice entry the framework itself uses for a row
 * tap (library-group restricted); it runs onClick, the click listener and the tree listener in
 * the same order the classic RecyclerView row does.
 */
@android.annotation.SuppressLint("RestrictedApi")
private fun Preference.performClickCompat() {
    performClick()
}
