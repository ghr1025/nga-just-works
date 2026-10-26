package gov.anzong.androidnga.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;

import sp.phone.common.User;
import sp.phone.common.UserManager;
import sp.phone.common.UserManagerImpl;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.justwen.androidnga.cloud.CloudServerManager;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.arouter.ARouterConstants;
import sp.phone.param.ParamKey;
import sp.phone.param.PostParam;
import sp.phone.ui.fragment.TopicPostFragment;

@Route(path = ARouterConstants.ACTIVITY_POST)
public class PostActivity extends BaseActivity {

    private TopicPostFragment mPostFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        setClassicOrMiuixContentView(R.layout.activity_post, R.id.content);
        setupToolbar();
        getOnBackPressedDispatcher().addCallback(this, new FragmentBackCallback(
                () -> mPostFragment != null && mPostFragment.isAdded() && mPostFragment.getView() != null,
                () -> mPostFragment.onBackPressed(),
                () -> getOnBackPressedDispatcher().onBackPressed()));
        PostParam act = getPostParam();
        if (act.getPostAction() == null) {
            finish();
            return;
        }
        setTitle(getTitleResId(act.getPostAction()));
        Bundle bundle = new Bundle();
        bundle.putString(ParamKey.KEY_ACTION, act.getPostAction());
        bundle.putParcelable("param", act);
        if (savedInstanceState != null) {
            bundle.putBundle("savedInstanceState", savedInstanceState);
        }
        mPostFragment = new TopicPostFragment();
        mPostFragment.setArguments(bundle);
        mPostFragment.setHasOptionsMenu(true);
        compatActivityAdjustResize(this);

        getSupportFragmentManager().beginTransaction().replace(R.id.content, mPostFragment).commit();
    }

    @Override
    protected boolean isMiuixPage() {
        return isMiuixStyle();
    }

    /**
     * The classic toolbar carries an account spinner. The Miuix top app bar has no slot for it,
     * so the same choice is offered as an options-menu entry while more than one account exists.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean created = super.onCreateOptionsMenu(menu);
        if (isMiuixPage() && UserManagerImpl.getInstance().getUserList().size() > 1) {
            menu.add(Menu.NONE, R.id.miuix_menu_switch_account, Menu.CATEGORY_SECONDARY, R.string.miuix_switch_account);
            created = true;
        }
        return created;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.miuix_menu_switch_account) {
            showAccountChooser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAccountChooser() {
        UserManager manager = UserManagerImpl.getInstance();
        java.util.List<User> users = manager.getUserList();
        CharSequence[] names = new CharSequence[users.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = users.get(i).getNickName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.miuix_switch_account)
                .setSingleChoiceItems(names, manager.getActiveUserIndex(), (dialog, which) -> {
                    if (which != manager.getActiveUserIndex()) {
                        manager.setActiveUser(which);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPostFragment.onSaveInstanceState(outState);
    }

    private int getTitleResId(String action) {
        if (action == null) {
            CloudServerManager.putCrashData(this, "post_intent", getIntent().toString());
        }
        switch (action) {
            case "reply":
                return R.string.reply_thread;
            case "modify":
                return R.string.modify_thread;
            default:
                return R.string.new_thread;
        }
    }

    private PostParam getPostParam() {
        Intent intent = getIntent();
        String tid = intent.getStringExtra(ParamKey.KEY_TID);
        int fid = intent.getIntExtra(ParamKey.KEY_FID, -7);
        String title = intent.getStringExtra("title");
        String pid = intent.getStringExtra(ParamKey.KEY_PID);
        String action = intent.getStringExtra(ParamKey.KEY_ACTION);
        String prefix = intent.getStringExtra("prefix");
        String stid = intent.getStringExtra(ParamKey.KEY_STID);
        if (prefix != null && prefix.startsWith("[quote][pid=") && prefix.endsWith("[/quote]\n")) {
            SpannableString spanString = new SpannableString(prefix);
            spanString.setSpan(new BackgroundColorSpan(-1513240), 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spanString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), prefix.indexOf("[b]Post by"), prefix.indexOf("):[/b]") + 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            prefix = spanString.toString();
        }
        PostParam act = new PostParam(tid, "", "");
        act.setPostAction(action);
        act.setPostFid(fid);
        act.setPostPid(pid);
        act.setPostContent(prefix);
        act.setPostSubject(title);
        act.setStid(stid);
        return act;
    }
}
