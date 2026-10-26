package sp.phone.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeKt;
import com.justwen.androidnga.ui.compose.miuix.MiuixFragment;

import java.util.List;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.widget.DividerItemDecorationEx;
import gov.anzong.androidnga.common.ui.dialog.ConfirmDialog;
import gov.anzong.androidnga.ui.miuix.MiuixTopicListBridge;
import gov.anzong.androidnga.ui.miuix.MiuixTopicListHost;
import gov.anzong.androidnga.ui.widget.ToolbarUtils;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.TopicHistoryManager;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.mvp.model.entity.TopicListInfo;
import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.ui.adapter.TopicListAdapter;
import sp.phone.view.RecyclerViewEx;

/**
 * Created by Justwen on 2018/1/17.
 */

public class TopicHistoryFragment extends BaseFragment implements View.OnClickListener, MiuixFragment,
        MiuixTopicListHost {

    private TopicListAdapter mTopicListAdapter;

    private RecyclerViewEx mListView;

    private TopicHistoryManager mTopicHistoryManager;

    /** Non null while the Miuix appearance renders this list. */
    private MiuixTopicListBridge mMiuix;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mTopicHistoryManager = TopicHistoryManager.getInstance();
        setTitle(R.string.label_activity_topic_history);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View classic = inflater.inflate(R.layout.fragment_settings_user, container, false);
        if (!MiuixChromeKt.isMiuixHost(getActivity())) {
            return classic;
        }
        mMiuix = new MiuixTopicListBridge(this, this, false);
        return mMiuix.wrap(classic);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mTopicListAdapter = new TopicListAdapter(getContext());
        mTopicListAdapter.setOnClickListener(this);

        mListView = view.findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListView.setEmptyView(view.findViewById(R.id.empty_view));
        mListView.setAdapter(mTopicListAdapter);
        mListView.addItemDecoration(new DividerItemDecorationEx(view.getContext(), ContextUtils.getDimension(R.dimen.topic_list_item_padding), DividerItemDecoration.VERTICAL));

        super.onViewCreated(view, savedInstanceState);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0) {
                    mTopicHistoryManager.removeTopicHistory(position);
                    mTopicListAdapter.removeItem(position);
                }

            }
        });
        //将recycleView和ItemTouchHelper绑定
        touchHelper.attachToRecyclerView(mListView);
        setData(mTopicHistoryManager.getTopicHistoryList());
        if (mMiuix != null) {
            mTopicListAdapter.setNextPageEnabled(false);
            mMiuix.attach(mTopicListAdapter);
            mMiuix.markLoaded();
        }

        // 浏览历史是本地列表，没有东西可刷新，点标题只回顶部。工具栏属于宿主 Activity，
        // 这个 Fragment 的布局里没有。
        ToolbarUtils.setOnTitleClickListener(requireActivity().findViewById(R.id.toolbar),
                v -> mListView.scrollToPosition(0));
    }

    private void setData(List<ThreadPageInfo> topicLIst) {
        TopicListInfo listInfo = new TopicListInfo();
        listInfo.setThreadPageList(topicLIst);
        mTopicListAdapter.setData(listInfo.getThreadPageList());
    }

    // ---- Miuix page: same actions as the classic list (swipe to delete becomes long press) ----

    @Override
    public boolean isMiuixRefreshEnabled() {
        return false;
    }

    @Override
    public void onMiuixRefresh() {
    }

    @Override
    public void onMiuixLoadNextPage() {
    }

    @Override
    public void onMiuixItemClick(ThreadPageInfo info) {
        View tagged = new View(requireContext());
        tagged.setTag(info);
        onClick(tagged);
    }

    @Override
    public String miuixDeleteConfirmText(ThreadPageInfo info) {
        return "确认删除这条浏览历史吗";
    }

    @Override
    public void onMiuixDelete(ThreadPageInfo info) {
        // Same as the classic swipe handler: the adapter position indexes both lists.
        int position = -1;
        for (int i = 0; i < mTopicListAdapter.getItemCount(); i++) {
            if (mTopicListAdapter.getItem(i) == info) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            mTopicHistoryManager.removeTopicHistory(position);
            mTopicListAdapter.removeItem(position);
        }
    }

    @Override
    public void onMiuixTitleClick() {
        mListView.scrollToPosition(0);
        if (mMiuix != null) {
            mMiuix.scrollToTop();
        }
    }

    @Override
    public Runnable miuixFabAction() {
        return null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_black_list_option_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete_all) {
            ConfirmDialog.Companion.showConfirmDialog(getActivity(), "确认删除所有浏览历史吗", () -> {
                mTopicHistoryManager.removeAllTopicHistory();
                mTopicListAdapter.clear();
            });
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View view) {
        ThreadPageInfo info = (ThreadPageInfo) view.getTag();
        ArticleListParam param = new ArticleListParam();
        param.tid = info.getTid();
        param.page = info.getPage();
        param.title = info.getSubject();
        param.topicInfo = JSON.toJSONString(info);
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ParamKey.KEY_PARAM, param);
        intent.putExtras(bundle);
        intent.setClass(getContext(), PhoneConfiguration.getInstance().articleActivityClass);
        startActivity(intent);
    }
}
