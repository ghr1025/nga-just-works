package gov.anzong.androidnga.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import com.justwen.androidnga.ui.compose.miuix.MiuixChromeKt;
import com.justwen.androidnga.ui.compose.miuix.MiuixFragment;

import gov.anzong.androidnga.ui.miuix.MiuixPreferenceHost;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, MiuixFragment {

    /** Non null while the Miuix appearance renders this screen. */
    @Nullable
    private MiuixPreferenceHost mMiuixHost;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View classicView = super.onCreateView(inflater, container, savedInstanceState);
        if (!MiuixChromeKt.isMiuixHost(getActivity())) {
            return classicView;
        }
        mMiuixHost = new MiuixPreferenceHost(this);
        return mMiuixHost.wrap(classicView);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mMiuixHost != null) {
            mMiuixHost.attach();
        }
    }

    @Override
    public void onDestroyView() {
        if (mMiuixHost != null) {
            mMiuixHost.detach();
            mMiuixHost = null;
        }
        super.onDestroyView();
    }

    /** Re-reads every row of the Miuix page after preference state changed in code. */
    protected void refreshMiuixRows() {
        if (mMiuixHost != null) {
            mMiuixHost.refresh();
        }
    }

    protected void mapping(PreferenceGroup group) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference preference = group.getPreference(i);
            preference.setIconSpaceReserved(false);
            if (preference instanceof PreferenceGroup) {
                mapping((PreferenceGroup) preference);
            } else {
                preference.setOnPreferenceChangeListener(this);
            }
        }
    }

    protected void configPreference(String key, Preference.OnPreferenceChangeListener listener) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setOnPreferenceChangeListener(listener);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    protected void setTitle(CharSequence title) {
        if (getActivity() != null) {
            getActivity().setTitle(title);
        }
    }
}
