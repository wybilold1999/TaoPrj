package com.crazy.taolove.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.crazy.taolove.R;
import com.crazy.taolove.activity.base.BaseActivity;
import com.crazy.taolove.config.AppConstants;
import com.crazy.taolove.config.ValueKey;
import com.crazy.taolove.manager.AppManager;
import com.crazy.taolove.net.request.OSSImagUploadRequest;
import com.crazy.taolove.net.request.UploadCommentImgRequest;
import com.crazy.taolove.utils.CheckUtil;
import com.crazy.taolove.utils.PreferencesUtils;
import com.crazy.taolove.utils.ProgressDialogUtils;
import com.crazy.taolove.utils.ToastUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * @author wangyb
 * @Description:赠送vip
 * @Date:2015年7月13日下午2:21:46
 */
public class GiveVipActivity extends BaseActivity {

    public static final int CHOOSE_IMG_RESULT = 0;

    @BindView(R.id.skip_market)
    FancyButton mSkipMarket;
    @BindView(R.id.upload_img)
    FancyButton mUploadImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_give_vip);
        ButterKnife.bind(this);
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
    }

    /**
     * 设置事件
     */
    private void setupEvent() {
    }

    /**
     * 设置数据
     */
    private void setupData() {
        if (PreferencesUtils.getIsUploadCommentImg(this)) {
            mUploadImg.setEnabled(false);
            mUploadImg.setFocusBackgroundColor(getResources().getColor(R.color.gray_text));
            mUploadImg.setBackgroundColor(getResources().getColor(R.color.gray_text));
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

    @OnClick({R.id.skip_market, R.id.upload_img})
    public void onViewClicked(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.skip_market:
                AppManager.goToMarket(this, CheckUtil.getAppMetaData(this, "UMENG_CHANNEL"));
                break;
            case R.id.upload_img:
                intent.setClass(this, PhotoChoserActivity.class);
                intent.putExtra(ValueKey.DATA, 1);
                startActivityForResult(intent, CHOOSE_IMG_RESULT);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_IMG_RESULT) {
            List<String> imgUrls = data.getStringArrayListExtra(ValueKey.IMAGE_URL);
            if (null != imgUrls && imgUrls.size() > 0) {
                ProgressDialogUtils.getInstance(this).show(R.string.dialog_request_uploda);
                new OSSUploadImgTask().request(AppManager.getFederationToken().bucketName,
                        AppManager.getOSSFacePath(), imgUrls.get(0));
            }
        }
    }

    class OSSUploadImgTask extends OSSImagUploadRequest {
        @Override
        public void onPostExecute(String s) {
            String url = AppConstants.OSS_IMG_ENDPOINT + s;
            new UploadCImgTask().request(url);
        }

        @Override
        public void onErrorExecute(String error) {
            ProgressDialogUtils.getInstance(GiveVipActivity.this).dismiss();
        }
    }

    class UploadCImgTask extends UploadCommentImgRequest {
        @Override
        public void onPostExecute(String s) {
            ProgressDialogUtils.getInstance(GiveVipActivity.this).dismiss();
            ToastUtil.showMessage(s);
            PreferencesUtils.setIsUploadCommentImg(GiveVipActivity.this, true);
            mUploadImg.setEnabled(false);
            mUploadImg.setFocusBackgroundColor(getResources().getColor(R.color.gray_text));
            mUploadImg.setBackgroundColor(getResources().getColor(R.color.gray_text));
        }

        @Override
        public void onErrorExecute(String error) {
            ProgressDialogUtils.getInstance(GiveVipActivity.this).dismiss();
            ToastUtil.showMessage(error);
        }
    }
}
