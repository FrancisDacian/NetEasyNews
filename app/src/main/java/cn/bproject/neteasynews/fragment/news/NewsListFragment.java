package cn.bproject.neteasynews.fragment.news;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aspsine.irecyclerview.IRecyclerView;
import com.aspsine.irecyclerview.OnLoadMoreListener;
import com.aspsine.irecyclerview.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;

import cn.bproject.neteasynews.R;
import cn.bproject.neteasynews.Utils.DensityUtils;
import cn.bproject.neteasynews.Utils.LogUtils;
import cn.bproject.neteasynews.Utils.ThreadManager;
import cn.bproject.neteasynews.Utils.UIUtils;
import cn.bproject.neteasynews.activity.NewsDetailActivity;
import cn.bproject.neteasynews.activity.PicDetailActivity;
import cn.bproject.neteasynews.adapter.NewsListAdapter;
import cn.bproject.neteasynews.bean.NewsListNormalBean;
import cn.bproject.neteasynews.common.Api;
import cn.bproject.neteasynews.common.DefineView;
import cn.bproject.neteasynews.fragment.BaseFragment;
import cn.bproject.neteasynews.http.DataParse;
import cn.bproject.neteasynews.http.HttpCallbackListener;
import cn.bproject.neteasynews.http.HttpHelper;
import cn.bproject.neteasynews.widget.ClassicRefreshHeaderView;
import cn.bproject.neteasynews.widget.LoadMoreFooterView;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by Bei on 2016/12/25.
 */

public class NewsListFragment extends BaseFragment implements DefineView {

    private final String TAG = NewsListFragment.class.getSimpleName();
    private static final String KEY = "TID";
    private View mView;     // 布局视图
    private List<NewsListNormalBean> mNewsListNormalBeanList;   // 启动时获得的数据
    private List<NewsListNormalBean> newlist;   // 上拉刷新后获得的数据
    private int mStartIndex = 0;    // 请求数据的起始参数
    private String mUrl;        // 请求网络的url
    private ThreadManager.ThreadPool mThreadPool;   // 线程池
    private boolean isPullRefresh;
    private String tid; // 栏目频道id
    private FrameLayout mFramelayout_news_list;
    private LinearLayout mLoading;
    private LinearLayout mEmpty;
    private LinearLayout mError;
    private Button mBtn_retry;
    private IRecyclerView mIRecyclerView;
    private LoadMoreFooterView mLoadMoreFooterView;
    private NewsListAdapter mNewsListAdapter;


    /**
     * 从外部往Fragment中传参数的方法
     *
     * @param tid 频道id
     * @return
     */
    public static NewsListFragment newInstance(String tid) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY, tid);
        NewsListFragment fragment = new NewsListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // 获取屏幕的宽度
        WindowManager windowManager = (WindowManager) getActivity().getSystemService(WINDOW_SERVICE);
        // windowManager.getDefaultDisplay().getWidth();
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getMetrics(outMetrics);// 给白纸设置宽高
        long width = outMetrics.widthPixels;
        long height = outMetrics.heightPixels;
        LogUtils.d(TAG, "width: " + width + "     height: " + height);

        mView = inflater.inflate(R.layout.fragment_news_list, container, false);
        initView();
        initValidata();
        initListener();
        return mView;
    }


    @Override
    public void initView() {
        mIRecyclerView = (IRecyclerView) mView.findViewById(R.id.iRecyclerView);

        mIRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mLoadMoreFooterView = (LoadMoreFooterView) mIRecyclerView.getLoadMoreFooterView();
        ClassicRefreshHeaderView classicRefreshHeaderView = new ClassicRefreshHeaderView(getActivity());
        classicRefreshHeaderView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DensityUtils.dip2px(getActivity(), 80)));
        // we can set view
        mIRecyclerView.setRefreshHeaderView(classicRefreshHeaderView);

        mFramelayout_news_list = (FrameLayout) mView.findViewById(R.id.framelayout_news_list);
        mLoading = (LinearLayout) mView.findViewById(R.id.loading);
        mEmpty = (LinearLayout) mView.findViewById(R.id.empty);
        mError = (LinearLayout) mView.findViewById(R.id.error);
        // 点击重试按键
        mBtn_retry = (Button) mView.findViewById(R.id.btn_retry);
    }

    @Override
    public void initValidata() {
        if (getArguments() != null) {
            //取出保存的频道TID
            tid = getArguments().getString("TID");
        }
        showLoadingPage();
        // 创建线程池
        mThreadPool = ThreadManager.getThreadPool();
        requestData();


    }

    public void requestData() {
        mUrl = Api.CommonUrl + tid + "/" + mStartIndex + Api.endUrl;
//        Log.d(TAG, "mUrl地址为: " + mUrl);
//        http://c.m.163.com/nc/article/list/T1467284926140/0-20.html
//        http://c.m.163.com/nc/article/list/T1348647909107/0-20.html

        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
//                CreateNewsProtocol();
                HttpHelper.get(mUrl, new HttpCallbackListener() {
                    @Override
                    public void onSuccess(String result) {
                        mNewsListNormalBeanList = DataParse.NewsList(result, tid);
                        UIUtils.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                LogUtils.d(TAG, ": 解析id" + tid);
                                if (mNewsListNormalBeanList != null) {
                                    showNewsPage();
                                    bindData();
                                } else {
                                    showEmptyPage();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String result, Exception e) {

                    }
                });

            }
        });

    }

    @Override
    public void initListener() {

        mIRecyclerView.setLoadMoreEnabled(true);
        mIRecyclerView.setRefreshEnabled(true);

        mIRecyclerView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                mUrl = Api.CommonUrl + tid + "/" + 0 + Api.endUrl;
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        HttpHelper.get(mUrl, new HttpCallbackListener() {
                            @Override
                            public void onSuccess(String result) {
                                newlist = DataParse.NewsList(result, tid);
                                isPullRefresh = true;
                                DataChange();
                            }

                            @Override
                            public void onError(String result, Exception e) {

                            }
                        });
                    }
                });
            }
        });

        mIRecyclerView.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                if (mLoadMoreFooterView.canLoadMore() && mNewsListAdapter.getItemCount() > 0) {
                    mLoadMoreFooterView.setStatus(LoadMoreFooterView.Status.LOADING);
                    mStartIndex += 20;
                    mUrl = Api.CommonUrl + tid + "/" + mStartIndex + Api.endUrl;
                    mThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            HttpHelper.get(mUrl, new HttpCallbackListener() {
                                @Override
                                public void onSuccess(String result) {
                                    Log.d(TAG, "onSuccess: " + mUrl);
                                    newlist = DataParse.NewsList(result, tid);
                                    isPullRefresh = false;
                                    DataChange();
                                }

                                @Override
                                public void onError(String result, Exception e) {
                                    mLoadMoreFooterView.setStatus(LoadMoreFooterView.Status.ERROR);
                                    Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    @Override
    public void bindData() {
        mNewsListAdapter = new NewsListAdapter(getActivity(), (ArrayList<NewsListNormalBean>) mNewsListNormalBeanList);
        mIRecyclerView.setIAdapter(mNewsListAdapter);
        // 设置Item点击跳转事件
        mNewsListAdapter.setOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                NewsListNormalBean newsListNormalBean = mNewsListNormalBeanList.get(position);
                String photosetID = newsListNormalBean.getPhotosetID();
                Intent intent;
                if (photosetID != null) {
                    intent = new Intent(getActivity(), PicDetailActivity.class);
                    String[] str = photosetID.split("\\|");
                    //  图片新闻文章所属的类目id
                    String tid = str[0].substring(4);
                    // 图片新闻的文章id好
                    String setid = str[1];
                    intent.putExtra("TID", tid);
                    intent.putExtra("SETID", setid);
                    LogUtils.d(TAG, "onItemClick: photosetID:" + photosetID);
                } else {
                    intent = new Intent(getActivity(), NewsDetailActivity.class);
                    intent.putExtra("DOCID", newsListNormalBean.getDocid());

                }
                getActivity().startActivity(intent);
            }
        });

}


    /**
     * 上拉或下拉刷新之后更新UI界面
     */
    private void DataChange() {
        UIUtils.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (newlist != null) {
                    isPullRefreshView();
                    Toast.makeText(getActivity(), "数据已更新", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "数据请求失败", Toast.LENGTH_SHORT).show();
                }
                mLoadMoreFooterView.setStatus(LoadMoreFooterView.Status.GONE);
                mIRecyclerView.setRefreshing(false);
            }
        });
    }

    /**
     * 判断是上拉刷新还是下拉刷新，执行相应的方法
     */
    public void isPullRefreshView() {
        if (isPullRefresh) {
            // 是下拉刷新
            newlist.addAll(mNewsListNormalBeanList);
            mNewsListNormalBeanList.removeAll(mNewsListNormalBeanList);
            mNewsListNormalBeanList.addAll(newlist);
            mNewsListAdapter.notifyDataSetChanged();
        } else {
            // 上拉刷新
            mNewsListNormalBeanList.addAll(newlist);
            mNewsListAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 如果有新闻就展示新闻页面
     */
    private void showNewsPage() {
        mIRecyclerView.setVisibility(View.VISIBLE);
        mFramelayout_news_list.setVisibility(View.GONE);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
    }

    /**
     * 展示加载页面
     */
    private void showLoadingPage() {
        mIRecyclerView.setVisibility(View.GONE);
        mFramelayout_news_list.setVisibility(View.VISIBLE);
        mLoading.setVisibility(View.VISIBLE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);

    }

    /**
     * 如果没有网络就展示空消息页面
     */
    private void showEmptyPage() {
        mIRecyclerView.setVisibility(View.GONE);
        mFramelayout_news_list.setVisibility(View.VISIBLE);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(View.VISIBLE);
        mError.setVisibility(View.GONE);

    }

    private void showErroPage() {
        mIRecyclerView.setVisibility(View.GONE);
        mFramelayout_news_list.setVisibility(View.VISIBLE);
        mLoading.setVisibility(View.GONE);
        mEmpty.setVisibility(View.GONE);
        mError.setVisibility(View.VISIBLE);

    }

}
