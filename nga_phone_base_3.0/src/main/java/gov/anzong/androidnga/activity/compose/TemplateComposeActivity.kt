package gov.anzong.androidnga.activity.compose

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.justwen.androidnga.ui.compose.BaseComposeFragment
import com.justwen.androidnga.ui.compose.miuix.NgaUiStyle

class TemplateComposeActivity : FragmentActivity() {

    /** Appearance the hosted fragment was built with. */
    private var miuixStyle = false

    override fun onCreate(savedInstanceState: Bundle?) {
        miuixStyle = NgaUiStyle.isMiuix()
        super.onCreate(savedInstanceState)
        val fragmentStr = intent.getStringExtra("fragment")
        if (fragmentStr != null) {
            commitFragment(fragmentStr)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing && miuixStyle != NgaUiStyle.isMiuix()) {
            recreate()
        }
    }

    private fun commitFragment(fragmentStr: String) {
        try {
            val fragment :BaseComposeFragment = Class.forName(fragmentStr).getDeclaredConstructor().newInstance() as BaseComposeFragment
            fragment.arguments = intent.extras
            supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commit()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

}