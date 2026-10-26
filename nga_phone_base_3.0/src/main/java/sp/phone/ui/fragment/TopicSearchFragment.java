package sp.phone.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alibaba.android.arouter.launcher.ARouter;
import com.justwen.androidnga.ui.compose.miuix.MiuixChromeKt;
import com.justwen.androidnga.ui.compose.miuix.MiuixFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import gov.anzong.androidnga.R;
import gov.anzong.androidnga.activity.BaseActivity;
import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.widget.DividerItemDecorationEx;
import gov.anzong.androidnga.ui.miuix.MiuixTopicListBridge;
import gov.anzong.androidnga.ui.miuix.MiuixTopicListHost;
import gov.anzong.androidnga.ui.widget.ToolbarUtils;
import sp.phone.common.ApiConstants;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.TopicHistoryManager;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.mvp.model.entity.TopicListInfo;
import sp.phone.mvp.model.thread.ArticleNavigation;
import sp.phone.mvp.presenter.TopicListPresenter;
import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.param.TopicListParam;
import sp.phone.ui.adapter.BaseAppendableAdapter;
import sp.phone.ui.adapter.ReplyListAdapter;
import sp.phone.ui.adapter.TopicListAdapter;
import sp.phone.util.ARouterUtils;
import sp.phone.util.StringUtils;
import sp.phone.view.LoadingLayout;
import sp.phone.view.RecyclerViewEx;

public class TopicSearchFragment extends BaseFragment implements View.OnClickListener, MiuixFragment,
        MiuixTopicListHost {

    private static final String TAG = TopicSearchFragment.class.getSimpleName();

    public static final int REQUEST_IMPORT_CACHE = 0;

    protected TopicListParam mRequestParam;

    protected BaseAppendableAdapter mAdapter;

    protected TopicListInfo mTopicListInfo;

    @BindView(R.id.swipe_refresh)
    public SwipeRefreshLayout mSwipeRefreshLayout;

    @BindView(R.id.list)
    public RecyclerViewEx mListView;

    @BindView(R.id.loading_view)
    public LoadingLayout mLoadingView;

    protected TopicListPresenter mPresenter;

    /** Non null while the Miuix appearance renders this list; mirrors {@link #mAdapter}. */
    protected MiuixTopicListBridge mMiuix;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mRequestParam = getArguments().getParcelable(ParamKey.KEY_PARAM);
        super.onCreate(savedInstanceState);
        setTitle();
        mPresenter = onCreatePresenter();
        getLifecycle().addObserver(mPresenter);
    }

    protected TopicListPresenter onCreatePresenter() {
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        TopicListPresenter topicListPresenter = viewModelProvider.get(TopicListPresenter.class);
        topicListPresenter.setRequestParam(mRequestParam);
        return topicListPresenter;
    }

    protected void setTitle() {
        if (!StringUtils.isEmpty(mRequestParam.key)) {
            if (mRequestParam.content == 1) {
                if (!StringUtils.isEmpty(mRequestParam.fidGroup)) {
                    setTitle("搜索全站(包含正文):" + mRequestParam.key);
                } else {
                    setTitle("搜索(包含正文):" + mRequestParam.key);
                }
            } else {
                if (!StringUtils.isEmpty(mRequestParam.fidGroup)) {
                    setTitle("搜索全站:" + mRequestParam.key);
                } else {
                    setTitle("搜索:" + mRequestParam.key);
                }
            }
        } else if (!StringUtils.isEmpty(mRequestParam.author)) {
            if (mRequestParam.searchPost > 0) {
                final String title = "搜索" + mRequestParam.author + "的回复";
                setTitle(title);
            } else {
                final String title = "搜索" + mRequestParam.author + "的主题";
                setTitle(title);
            }
        } else if (mRequestParam.recommend == 1) {
            setTitle(mRequestParam.title + " - 精华区");
        } else if (mRequestParam.twentyfour == 1) {
            setTitle(mRequestParam.title + " - 24小时热帖");
        } else if (!TextUtils.isEmpty(mRequestParam.title)) {
            setTitle(mRequestParam.title);
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int layoutId = R.layout.fragment_topic_list;
        return wrapMiuix(inflater.inflate(layoutId, container, false));
    }

    /**
     * Wraps the classic view in a Miuix page when the Miuix appearance is active. The classic
     * views stay (hidden) so ButterKnife bindings, the adapter and every observer keep working;
     * the Miuix page renders what the adapter holds.
     */
    protected View wrapMiuix(View classicView) {
        if (!MiuixChromeKt.isMiuixHost(getActivity())) {
            return classicView;
        }
        mMiuix = new MiuixTopicListBridge(this, this, mRequestParam.searchPost > 0);
        return mMiuix.wrap(classicView);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mLoadingView.bindToLifecycle(getViewLifecycleOwner());
        ((BaseActivity) getActivity()).setupToolbar();

        if (mRequestParam.searchPost > 0) {
            mAdapter = new ReplyListAdapter(getContext());
            mListView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        } else {

            mAdapter = new TopicListAdapter(getContext());
        }

        mAdapter.setOnClickListener(this);

        mListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListView.setOnNextPageLoadListener(new RecyclerViewEx.OnNextPageLoadListener() {
            @Override
            public void loadNextPage() {
                if (!isRefreshing()) {
                    mPresenter.loadNextPage(mAdapter.getNextPage(), mRequestParam);
                }
            }
        });
        mListView.setEmptyView(view.findViewById(R.id.empty_view));
        mListView.setAdapter(mAdapter);
        if (mMiuix != null) {
            mMiuix.attach(mAdapter);
        }
        if (PhoneConfiguration.getInstance().useSolidColorBackground()) {
            int padding = PhoneConfiguration.getInstance().useSolidColorBackground() ? ContextUtils.getDimension(R.dimen.topic_list_item_padding) : 0;
            mListView.addItemDecoration(new DividerItemDecorationEx(view.getContext(), padding, DividerItemDecoration.VERTICAL));
        }

        mSwipeRefreshLayout.setVisibility(View.GONE);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.loadPage(1, mRequestParam);
            }
        });

        super.onViewCreated(view, savedInstanceState);

        mPresenter.getFirstTopicList().observe(getViewLifecycleOwner(), topicListInfo -> {
            scrollTo(0);
            clearData();
            if (topicListInfo != null) {
                setData(topicListInfo);
            }
        });

        mPresenter.getNextTopicList().observe(getViewLifecycleOwner(), this::setData);

        mPresenter.getErrorMsg().observe(getViewLifecycleOwner(), res -> {
            showToast(res);
            setNextPageEnabled(false);
        });

        mPresenter.isRefreshing().observe(getViewLifecycleOwner(), aBoolean -> {
            setRefreshing(aBoolean);
            if (!aBoolean) {
                hideLoadingView();
            }
            if (mMiuix != null) {
                mMiuix.setRefreshing(aBoolean);
            }
        });

        ToolbarUtils.setOnTitleClickListener(view.findViewById(R.id.toolbar), v -> onTitleClick());
    }

    /**
     * 点标题回到顶部并刷新。
     */
    protected void onTitleClick() {
        scrollToTopAndRefresh();
    }

    /**
     * 回到顶部并重新加载第一页。缓存列表加载完会关掉下拉刷新，这里跟着一起停，只回顶部。
     */
    protected void scrollToTopAndRefresh() {
        scrollTo(0);
        if (mSwipeRefreshLayout.isEnabled() && !isRefreshing()) {
            mPresenter.loadPage(1, mRequestParam);
        }
    }

    /** Pull-to-refresh of the Miuix page; mirrors the classic SwipeRefreshLayout listener. */
    @Override
    public void onMiuixRefresh() {
        if (mSwipeRefreshLayout.isEnabled() && !isRefreshing()) {
            mPresenter.loadPage(1, mRequestParam);
        } else if (mMiuix != null) {
            mMiuix.setRefreshing(false);
        }
    }

    /** End of the Miuix list reached; mirrors RecyclerViewEx's next page trigger. */
    @Override
    public void onMiuixLoadNextPage() {
        if (!isRefreshing() && mAdapter.hasNextPage()) {
            mPresenter.loadNextPage(mAdapter.getNextPage(), mRequestParam);
        }
    }

    /** Whether pull-to-refresh applies; caches disable it after loading. */
    @Override
    public boolean isMiuixRefreshEnabled() {
        return mSwipeRefreshLayout == null || mSwipeRefreshLayout.isEnabled();
    }

    /** Title tap on the Miuix top app bar; same action as the classic toolbar title. */
    @Override
    public void onMiuixTitleClick() {
        onTitleClick();
    }

    /** Row tap on the Miuix page; the classic adapter passes the item through the view tag. */
    @Override
    public void onMiuixItemClick(ThreadPageInfo info) {
        View tagged = new View(requireContext());
        tagged.setTag(info);
        onClick(tagged);
    }

    /**
     * Long-press delete confirmation for the Miuix page, or null when rows cannot be deleted.
     * Subclasses that delete on long press return their confirmation text.
     */
    @Override
    public String miuixDeleteConfirmText(ThreadPageInfo info) {
        return null;
    }

    /** Confirmed long-press delete on the Miuix page. */
    @Override
    public void onMiuixDelete(ThreadPageInfo info) {
    }

    /** Primary action of the Miuix page (the board list's post button), or null. */
    @Override
    public Runnable miuixFabAction() {
        return null;
    }



    public void scrollTo(int position) {
        mListView.scrollToPosition(position);
        if (position == 0 && mMiuix != null) {
            mMiuix.scrollToTop();
        }
    }

    public void setNextPageEnabled(boolean enabled) {
        mAdapter.setNextPageEnabled(enabled);
    }

    public void removeTopic(int position) {

    }

    public void removeTopic(ThreadPageInfo pageInfo) {

    }

    public void hideLoadingView() {
        if (mLoadingView.getVisibility() == View.VISIBLE) {
            mLoadingView.setVisibility(View.GONE);
            mSwipeRefreshLayout.setVisibility(View.VISIBLE);
        }
    }

    public void setRefreshing(boolean refreshing) {
        if (mSwipeRefreshLayout.getVisibility() == View.VISIBLE) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isShown() ? mSwipeRefreshLayout.isRefreshing() : mLoadingView.isShown();
    }

    public void setData(TopicListInfo result) {
        mTopicListInfo = result;
        mAdapter.setData(result.getThreadPageList());
    }

    public void clearData() {
        mAdapter.setData(null);
    }

    @Override
    public void onClick(View view) {
        ThreadPageInfo info = (ThreadPageInfo) view.getTag();
        handleClickEvent(view.getContext(), info, mRequestParam);
    }

    public static void handleClickEvent(Context context, ThreadPageInfo info, TopicListParam requestParam) {

        if (info.isMirrorBoard()) {
            ARouterUtils.build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                    .withInt(ParamKey.KEY_FID, info.getFid())
                    .withString(ParamKey.KEY_TITLE, info.getSubject())
                    .navigation(context);
        } else if ((info.getType() & ApiConstants.MASK_TYPE_ASSEMBLE) == ApiConstants.MASK_TYPE_ASSEMBLE) {
            TopicListParam param = new TopicListParam();
            param.title = info.getSubject();
            param.stid = info.getTid();
            ARouter.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                    .withParcelable(ParamKey.KEY_PARAM, param)
                    .navigation();

        } else {

            ArticleListParam param = ArticleNavigation.fromSearchResult(info, requestParam);

            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putParcelable(ParamKey.KEY_PARAM, param);
            intent.putExtras(bundle);
            intent.setClass(context, PhoneConfiguration.getInstance().articleActivityClass);
            context. startActivity(intent);
            TopicHistoryManager.getInstance().addTopicHistory(info);
        }
    }


}
