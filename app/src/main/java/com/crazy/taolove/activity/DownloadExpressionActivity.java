package com.crazy.taolove.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.crazy.taolove.R;
import com.crazy.taolove.activity.base.BaseActivity;
import com.crazy.taolove.adapter.DownloadExpressionAdapter;
import com.crazy.taolove.db.ExpressionGroupSqlManager;
import com.crazy.taolove.entity.ExpressionGroup;
import com.crazy.taolove.listener.DownloadProgressExpressionListener;
import com.crazy.taolove.listener.DownloadProgressExpressionListener.OnExpressionProgressChangedListener;
import com.crazy.taolove.listener.UserStickerPackListener;
import com.crazy.taolove.listener.UserStickerPackListener.OnUserStickerPackListener;
import com.crazy.taolove.net.request.ExpressionRequest;
import com.crazy.taolove.ui.widget.DividerItemDecoration;
import com.crazy.taolove.utils.DensityUtil;
import com.crazy.taolove.utils.ProgressDialogUtils;
import com.crazy.taolove.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;

/**
 * 
 * @ClassName:DownloadExpressionActivity
 * @Description:下载表情
 * @author zxj
 * @Date:2015年6月12日上午11:08:54
 */
public class DownloadExpressionActivity extends BaseActivity implements
        OnRefreshListener, OnExpressionProgressChangedListener,
		OnUserStickerPackListener {

	private SwipeRefreshLayout mSwipeRefresh;
	private RecyclerView mRecyclerView;
	private DownloadExpressionAdapter mAdapter;

	private List<ExpressionGroup> mExpressionGroups;

	/** 已有表情id */
	private List<ExpressionGroup> mPresenceExpression;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download_expression);
		Toolbar toolbar = getActionBarToolbar();
		if (toolbar != null) {
			toolbar.setNavigationIcon(R.mipmap.ic_up);
		}
		setupViews();
		setupEvent();
		setupData();
	}

	/**
	 * 设置视图
	 */
	private void setupViews() {
		mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
		mSwipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
				android.R.color.holo_green_light,
				android.R.color.holo_orange_light,
				android.R.color.holo_red_light);
		mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
				LinearLayoutManager.VERTICAL, DensityUtil.dip2px(this, 12),
				DensityUtil.dip2px(this, 12)));
	}

	/**
	 * 设置事件监听
	 */
	private void setupEvent() {
		mSwipeRefresh.setOnRefreshListener(this);
		DownloadProgressExpressionListener.getInstance()
				.addOnExpressionProgressChangedListener(this);
		UserStickerPackListener.getInstance()
				.addOnUserStickerPackListener(this);
	}

	/**
	 * 设置数据
	 */
	private void setupData() {
		mExpressionGroups = new ArrayList<ExpressionGroup>();
		mPresenceExpression = new ArrayList<ExpressionGroup>();
		List<ExpressionGroup> list = ExpressionGroupSqlManager.getInstance(this).getExpressionGroup();
		if (list != null) {
			mPresenceExpression = list;
		}

		mAdapter = new DownloadExpressionAdapter(mExpressionGroups, false);
		mRecyclerView.setAdapter(mAdapter);
		ProgressDialogUtils.getInstance(this)
				.show(R.string.dialog_request_data);
		new ExpressionTask().request();

	}

	/**
	 * 在线表情数据请求
	 */
	class ExpressionTask extends ExpressionRequest {
		@Override
		public void onPostExecute(List<ExpressionGroup> result) {
			ProgressDialogUtils.getInstance(DownloadExpressionActivity.this)
					.dismiss();
			if (result != null) {
				mExpressionGroups.clear();
				mExpressionGroups.addAll(result);
				// 遍历处理已经下载过的表情
				for (int i = 0; i < mExpressionGroups.size(); i++) {
					for (int j = 0; j < mPresenceExpression.size(); j++) {
						if (mExpressionGroups.get(i).id_pic_themes == mPresenceExpression
								.get(j).id_pic_themes) {
							mExpressionGroups.get(i).status = ExpressionGroup.ExpressionGroupStatus.ALREADY_DOWNLOAD;
						}
					}
				}
				mAdapter.notifyDataSetChanged();
			}
			mSwipeRefresh.setRefreshing(false);
		}

		@Override
		public void onErrorExecute(String error) {
			ToastUtil.showMessage(error);
			ProgressDialogUtils.getInstance(DownloadExpressionActivity.this)
					.dismiss();
		}
	}

	@Override
	public void onRefresh() {
		new ExpressionTask().request();
	}

	@Override
	public void onDownloadExpressionProgressChanged(
			ExpressionGroup expressionGroup, int progress,
			boolean is_changed_down_view) {
		if (!is_changed_down_view) {
			return;
		}
		int chatMessageIndex = -1;
		if (mExpressionGroups != null) {
			for (int i = 0; i < mExpressionGroups.size(); i++) {
				if (mExpressionGroups.get(i).id_pic_themes == expressionGroup.id_pic_themes) {
					expressionGroup.progress = progress;
					if (progress >= 100) {
						expressionGroup.status = ExpressionGroup.ExpressionGroupStatus.ALREADY_DOWNLOAD;
						mPresenceExpression.add(expressionGroup);
					}
					mExpressionGroups.set(i, expressionGroup);
					chatMessageIndex = i;
					break;
				}
			}
		}
		LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView
				.getLayoutManager();
		int firstVisibleItemIndex = layoutManager
				.findFirstVisibleItemPosition();
		int lastVisibleItemIndex = layoutManager.findLastVisibleItemPosition();
		if (chatMessageIndex >= 0 && chatMessageIndex >= firstVisibleItemIndex
				&& chatMessageIndex <= lastVisibleItemIndex) {
			int offset = chatMessageIndex - firstVisibleItemIndex;
			View view = mRecyclerView.getChildAt(offset);
			if (null == view) {
				return;
			}
			ProgressBar pb = (ProgressBar) view
					.findViewById(R.id.download_progress);
			TextView already_download = (TextView) view
					.findViewById(R.id.already_download);
			FancyButton download_expression = (FancyButton) view
					.findViewById(R.id.download_expression);

			if (pb != null && already_download != null
					&& download_expression != null) {
				pb.setVisibility(View.GONE);
				already_download.setVisibility(View.GONE);
				download_expression.setVisibility(View.GONE);
				if (progress < 100) {
					pb.setVisibility(View.VISIBLE);
					pb.setProgress(progress);
				} else {
					already_download.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onPageStart(this.getClass().getName());
		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPageEnd(this.getClass().getName());
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		DownloadProgressExpressionListener.getInstance()
				.removeOnExpressionProgressChangedListener(this);
		UserStickerPackListener.getInstance().removeOnUserStickerPackListener(
				this);
	}

	@Override
	public void onDeleteStickerPack(int packId) {

		for (int i = 0; i < mPresenceExpression.size(); i++) {
			if (mPresenceExpression.get(i).id_pic_themes == packId) {
				mPresenceExpression.remove(i);
				break;
			}
		}

		// 遍历处理已经下载过的表情
		for (int i = 0; i < mExpressionGroups.size(); i++) {
			ExpressionGroup eg = mExpressionGroups.get(i);
			if (eg.id_pic_themes == packId) {
				eg.status = ExpressionGroup.ExpressionGroupStatus.NO_DOWNLOAD;
				mExpressionGroups.set(i, eg);
				mAdapter.notifyItemChanged(i);
				break;
			}
		}

	}
}
