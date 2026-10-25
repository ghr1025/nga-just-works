package gov.anzong.androidnga.activity;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.justwen.androidnga.ui.compose.miuix.MiuixFragment;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.ui.fragment.BasePreferenceFragment;
import sp.phone.ui.fragment.BaseFragment;

public class LauncherSubActivity extends BaseActivity {

    private BaseFragment mBaseFragment;

    /** Resolved before super.onCreate so the host can pick its Miuix or classic chrome. */
    @Nullable
    private Class<?> mFragmentClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        Intent intent = getIntent();
        String fragmentStr = intent.getStringExtra("fragment");
        mFragmentClass = resolveFragmentClass(fragmentStr);
        super.onCreate(savedInstanceState);
        if (isMiuixPage()) {
            setMiuixContentView(R.id.main_content);
        } else {
            setContentView(R.layout.activity_toolbar_template);
            setupToolbar();
        }
        getOnBackPressedDispatcher().addCallback(this, new FragmentBackCallback(
                () -> mBaseFragment != null && mBaseFragment.isAdded() && mBaseFragment.getView() != null,
                () -> mBaseFragment.onBackPressed(),
                () -> getOnBackPressedDispatcher().onBackPressed()));
        if (fragmentStr != null) {
            commitFragment(fragmentStr);
        }
    }

    @Nullable
    private static Class<?> resolveFragmentClass(@Nullable String fragmentStr) {
        if (fragmentStr == null) {
            return null;
        }
        try {
            return Class.forName(fragmentStr);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    protected boolean isMiuixPage() {
        return isMiuixStyle() && mFragmentClass != null && MiuixFragment.class.isAssignableFrom(mFragmentClass);
    }

    private void commitFragment(String fragmentStr) {
        try {
            Object fragment = Class.forName(fragmentStr).newInstance();
            if (fragment instanceof BaseFragment) {
                mBaseFragment = (BaseFragment) fragment;
                Bundle bundle = getIntent().getExtras();
                mBaseFragment.setArguments(bundle);
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, mBaseFragment).commit();
            } else if (fragment instanceof BasePreferenceFragment) {
                getSupportFragmentManager().beginTransaction().replace(R.id.main_content, (BasePreferenceFragment)fragment).commit();
            } else {
                Bundle bundle = getIntent().getExtras();
                ((Fragment) fragment).setArguments(bundle);
                getFragmentManager().beginTransaction().replace(R.id.main_content, (Fragment) fragment).commit();
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
