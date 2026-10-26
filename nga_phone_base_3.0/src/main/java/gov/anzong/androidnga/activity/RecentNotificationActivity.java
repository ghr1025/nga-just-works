package gov.anzong.androidnga.activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.alibaba.android.arouter.facade.annotation.Route;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.arouter.ARouterConstants;
import sp.phone.ui.fragment.RecentNotificationFragment;

@Route(path = ARouterConstants.ACTIVITY_NOTIFICATION)
public class RecentNotificationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        setClassicOrMiuixContentView(R.layout.activity_toolbar_template, R.id.main_content);
        setupToolbar();
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.main_content);
        if (fragment == null) {
            fragment = new RecentNotificationFragment();
            fragment.setArguments(getIntent().getExtras());
            fm.beginTransaction().add(R.id.main_content, fragment).commit();
        }
        setTitle("我的被喷");
    }

    @Override
    protected boolean isMiuixPage() {
        return isMiuixStyle();
    }
}
