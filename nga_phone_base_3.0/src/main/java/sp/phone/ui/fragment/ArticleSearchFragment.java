package sp.phone.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.justwen.androidnga.ui.compose.miuix.MiuixChromeKt;
import com.justwen.androidnga.ui.compose.miuix.MiuixFragment;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.ui.miuix.MiuixReaderCallbacks;
import gov.anzong.androidnga.ui.miuix.MiuixReaderShellKt;
import gov.anzong.androidnga.ui.miuix.MiuixReaderTabsState;
import kotlin.Unit;
import sp.phone.common.PhoneConfiguration;
import sp.phone.param.ParamKey;
import sp.phone.param.ArticleListParam;

/**
 * Created by Justwen on 2017/11/22.
 */

public class ArticleSearchFragment extends ArticleListFragment implements MiuixFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View classic = inflater.inflate(R.layout.fragment_article_list_reply, container, false);
        if (!MiuixChromeKt.isMiuixHost(getActivity())) {
            return classic;
        }
        // A single reply page: Miuix top app bar, no page tabs and no reply button.
        MiuixReaderCallbacks callbacks = new MiuixReaderCallbacks(
                index -> Unit.INSTANCE, () -> Unit.INSTANCE, null, null, null, 1L);
        return MiuixReaderShellKt.miuixReaderShell(classic, classic.findViewById(R.id.swipe_refresh),
                requireActivity(), new MiuixReaderTabsState(), callbacks);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.article_list_option_menu_reply,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_show_whole_thread) {
            ArticleListParam full = fullThreadParam();
            if (full == null) { showToast("尚未确定帖子，请先加载内容"); return true; }
            Intent intentThis = new Intent();
            intentThis.putExtra(ParamKey.KEY_PARAM, full);
            intentThis.setClass(getActivity(), PhoneConfiguration.getInstance().articleActivityClass);
            startActivity(intentThis);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
