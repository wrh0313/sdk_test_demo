package cn.com.pcauto.video.uilibrary;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.bean.InfoCode;
import com.aliyun.player.source.UrlSource;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cn.com.pcauto.video.library.PCFollowVideoSDK;
import cn.com.pcauto.video.library.TakePhotoCallback;
import cn.com.pcauto.video.library.bumptech.glide.Glide;
import cn.com.pcauto.video.library.bumptech.glide.request.RequestOptions;
import cn.com.pcauto.video.library.common.downloader.TemplateDownloadManager;
import cn.com.pcauto.video.library.common.downloader.TemplateUtil;
import cn.com.pcauto.video.library.common.log.PCLog;
import cn.com.pcauto.video.library.common.network.http.core.HttpManagerCallback;
import cn.com.pcauto.video.library.common.video.AliyunRenderView;
import cn.com.pcauto.video.library.model.FollowVideoTemplateBean;
import cn.com.pcauto.video.library.model.RegisterKeyException;
import cn.com.pcauto.video.library.model.TemplateDetailBean;
import cn.com.pcauto.video.library.model.TemplateIno;
import cn.com.pcauto.video.library.module.base.BaseActivity;
import cn.com.pcauto.video.library.utils.DisplayUtils;
import cn.com.pcauto.video.library.utils.NetworkUtils;
import cn.com.pcauto.video.library.utils.StatusBarUtil;
import cn.com.pcauto.video.library.utils.ToastUtils;
import cn.com.pcauto.video.uilibrary.widget.StatusView;
import cn.com.pcauto.video.library.utils.ABISUtil;


public class TemplatePreviewActivity extends BaseActivity {

    private static final String TAG = TemplatePreviewActivity.class.getSimpleName();

    private static final String KEY_TEMPLATE_ID = "TOPIC_ID";
    private static final int CODE_REQ_PERMISSION = 0x200;

    private int mTemplateId;

    private StatusView mStatusView;

    private AliyunRenderView mPlayer;

    private TemplateIno mTemplateIno;

    private ImageView mIvBack;
    private ImageView mIvPlay;
    private ImageView mIvCover;
    private ImageView mIvLoading;
    private Toolbar mToolbar;
    private TextView mTvDownloadTemplate;

    private int mCurrentState = IPlayer.idle;
    private boolean mHasInit = false;
    private boolean mStartWhenOnResume = true;  //
    private boolean resumed = false;

    private TemplateDownloadManager mDownloadManager;

    public static void start(Context context, int templateId) {
        if (ABISUtil.showX86Tip(context)) {
            return;
        }
        Intent intent = getIntent(context, templateId);
        context.startActivity(intent);
    }

    public static Intent getIntent(Context context, int templateId) {
        Intent intent = new Intent(context, TemplatePreviewActivity.class);
        intent.putExtra(KEY_TEMPLATE_ID, templateId);
        return intent;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (ABISUtil.showX86Tip(this)) {
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getResId() {
        return R.layout.pc_lib_video_activity_template_preview;
    }

    @Override
    protected void initView() {
        StatusBarUtil.transparencyBar(this);
        mStatusView = findView(R.id.status_view);
        mPlayer = findView(R.id.player);
        mToolbar = findView(R.id.toolbar);
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams();
        int statusHeight = DisplayUtils.getStatusBarHeight(mContext);
        mToolbar.setPadding(0, statusHeight, 0, 0);
        lp.height += statusHeight;
        mIvBack = findView(R.id.iv_back);
        mIvPlay = findView(R.id.iv_play);
        mIvCover = findView(R.id.iv_cover);
        mIvLoading = findView(R.id.iv_loading);
        mTvDownloadTemplate = findViewById(R.id.tv_download_template);
        mStatusView.setColor(Color.WHITE);
        mStatusView.loading();

        mStatusView.setListener(new StatusView.StatusListener() {
            @Override
            public void onRetry() {
                mStatusView.loading();
                initData();
            }

        });


        mPlayer.setOnLoadingStatusListener(new ProgressListener(this));
        mPlayer.setOnPreparedListener(new PrePareListener(this));
        mPlayer.setOnStateChangedListener(new StateChangeListener(this));
        mPlayer.setOnInfoListener(new IPlayer.OnInfoListener() {
            @Override
            public void onInfo(InfoBean infoBean) {
                if (hasDestroy()) return;
                if (infoBean.getCode() == InfoCode.AutoPlayStart) {
                    if (!resumed) {
                        mPlayer.pause();
                    }
                }
            }
        });


        mPlayer.setAutoPlay(true);
        mPlayer.enableHardwareDecoder(true);

        // 播放器显示画面
        mPlayer.setSurfaceType(AliyunRenderView.SurfaceType.SURFACE_VIEW);

        // 循环播放
        mPlayer.setLoop(true);
        // 视频填充
        mPlayer.setScaleModel(IPlayer.ScaleMode.SCALE_ASPECT_FILL);
        // 基本配置
        mPlayer.initPlayerConfig();
//        mPlayer.initCacheConfig();
        //设置缓存
        String videoCacheDir = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            videoCacheDir = getExternalFilesDir("template_preview_video_cache").getAbsolutePath();
        } else {
            videoCacheDir = getFilesDir().getAbsolutePath() + File.separator + "template_preview_video_cache";
        }
        mPlayer.setCacheConfig(true, videoCacheDir, 60 * 60, 200);
        mPlayer.setOnVideoSizeChangedListener(new IPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int width, int height) {
                if (mPlayer == null) {
                    return;
                }
                if (width > height) {
                    mPlayer.setScaleModel(IPlayer.ScaleMode.SCALE_ASPECT_FIT);
                } else {
                    mPlayer.setScaleModel(IPlayer.ScaleMode.SCALE_ASPECT_FILL);
                }
            }
        });
    }

    @Override
    protected void bindEvent() {
        bindClickEvent(R.id.tv_download_template);
        bindClickEvent(R.id.iv_back);
        bindClickEvent(R.id.player);
    }


    @Override
    protected void initData() {
        getData(mTemplateId);

    }

    private void initConnectivityManager() {
        if (connMgr != null) return;
        connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null) {
            connMgr.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
        }
    }

    @Override
    protected void processClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_play) {   // 播放

        } else if (id == R.id.tv_download_template) {   // 下载模板
            onDownloadTemplate();
        } else if (id == R.id.player) {   // 点击视频区域
            if (mCurrentState == IPlayer.started) { // 暂停视频
                mStartWhenOnResume = false;
                mPlayer.pause();
            } else if (mCurrentState == IPlayer.paused) {
                mStartWhenOnResume = true;
                mPlayer.start();
            }
        } else if (id == R.id.iv_back) {   // 返回
            onBackPressed();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        pause();
    }

    @Override
    protected void getBundleData(@Nullable Bundle bundle) {
        if (bundle != null) {
            mTemplateId = bundle.getInt(KEY_TEMPLATE_ID);
        }
    }

    private void getData(int templateId) {
        PCFollowVideoSDK.getTemplateInfoById(templateId, new HttpManagerCallback<TemplateDetailBean>(TemplateDetailBean.class) {
            @Override
            public void onResponse(TemplateDetailBean result) {

                if (hasDestroy()) return;
                if (result != null && result.isSuccess() && result.getData() != null) {
                    PCLog.onEvent("PreDataSuccess");
                    mStatusView.loaded();
                    TemplateIno data = result.getData();
                    mTemplateIno = data;
                    startLoadStencil();
                    updateData(data);
                } else {
                    PCLog.onEvent(String.format(Locale.CHINA, "PreDataError(%s)", "response data is null"));
//                    loadFailed();
                    mStatusView.noData(R.drawable.pc_lib_video_error, "暂无数据");
                }
                initConnectivityManager();
            }

            @Override
            public void onFailure(Exception e) {
                if (hasDestroy()) return;

                if (e instanceof RegisterKeyException) {
                    if ("400".equals(((RegisterKeyException) e).code)) {
                        ToastUtils.show(mContext, "key失效，请联系客服");
                    }
                    PCLog.onEvent(String.format(Locale.CHINA, "PreDataError(%s(%s))", e.getMessage(), ((RegisterKeyException) e).code));
                } else {
                    PCLog.onEvent(String.format(Locale.CHINA, "PreDataError(%s)", e == null ? "" : e.getMessage()));
                }
                loadFailed();
                initConnectivityManager();
            }
        });
    }

    private void loadFailed() {
        mStatusView.networkError();
    }

    private void updateData(TemplateIno template) {
        // 返回键
        mIvBack.setImageResource(R.drawable.pc_lib_video_img_ask_detail_back);
        mToolbar.setBackgroundColor(Color.TRANSPARENT);

        // 标题
        TextView tvTitle = findView(R.id.tv_nav_text);
        String title = template.getName();
        if (title != null && title.length() > 13) {
            title = title.substring(0, 13) + "...";
        }
        tvTitle.setText(title);

        // 时长
        TextView tvDuration = findView(R.id.tv_duration);
        if (template.isVideoType()) {
            tvDuration.setVisibility(View.VISIBLE);
            tvDuration.setText(String.format("时长:  %s", getFormatTime(template.getDuration())));
        } else {
            tvDuration.setVisibility(View.INVISIBLE);
        }


        // 分镜数
        TextView tvStoryBoardCount = findView(R.id.tv_story_board_count);
        if (template.isVideoType()) {
            tvStoryBoardCount.setVisibility(View.VISIBLE);
            tvStoryBoardCount.setText(String.format(Locale.CHINA, "%d个分镜", template.getFjNum()));
        } else {
            tvStoryBoardCount.setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.line).setVisibility(template.isVideoType() ? View.VISIBLE : View.INVISIBLE);

        // 封面
        if (template.isVideoType()) {
            Glide.with(mContext)
                    .load(template.getCover())
                    .apply(new RequestOptions().centerCrop())
                    .into(mIvCover);

        } else {
            Glide.with(mContext)
                    .load(template.getCover())
                    .into(mIvCover);
            mIvPlay.setVisibility(View.GONE);
        }

        if (template.isVideoType()) {
            showAnimation(true);
            initPlayer(template);
        } else {
            updateDownloadTemplateUi();
        }
    }


    private void initPlayer(TemplateIno template) {
        UrlSource urlSource = null;
        if (!TextUtils.isEmpty(template.getVideoUrl())) {
            urlSource = mPlayer.getUrlSource(template.getVideoUrl());
        }

        if (urlSource == null) {
            return;
        }
        // prepare 后,自动播放
        mPlayer.setAutoPlay(true);
        mPlayer.setDataSource(urlSource);
        mPlayer.prepare();

    }

    private String getFormatTime(long seconds) {
        String formatTime = "未知";
        if (seconds < 60 * 60) {
            long minutes = TimeUnit.SECONDS.toMinutes(seconds);
            formatTime = String.format(Locale.CHINA, "%02d:%02d", minutes, seconds - TimeUnit.MINUTES.toSeconds(minutes));
        } else if (seconds < 60 * 60 * 24) {
            long hours = TimeUnit.SECONDS.toHours(seconds);
            long minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(hours);
            formatTime = String.format(Locale.CHINA, "%01d:%02d:%02d", hours,
                    minutes, seconds - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours));
        }
        return formatTime;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        if (mDownloadManager != null) {
            mDownloadManager.destroy();
        }
        showAnimation(false);
        if (connMgr != null) {
            connMgr.unregisterNetworkCallback(networkCallback);
        }
    }


    private static class ProgressListener implements IPlayer.OnLoadingStatusListener {
        private final WeakReference<TemplatePreviewActivity> activityWeakReference;

        public ProgressListener(TemplatePreviewActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onLoadingBegin() {
            TemplatePreviewActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onLoadingBegin();
            }
        }

        @Override
        public void onLoadingProgress(int i, float v) {
            TemplatePreviewActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onLoadingProgress();
            }
        }

        @Override
        public void onLoadingEnd() {
            TemplatePreviewActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onLoadingEnd();
            }
        }
    }


    private static class PrePareListener implements IPlayer.OnPreparedListener {
        private final WeakReference<TemplatePreviewActivity> activityWeakReference;

        public PrePareListener(TemplatePreviewActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onPrepared() {
            TemplatePreviewActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onPrepared();
            }
        }
    }

    private static class StateChangeListener implements IPlayer.OnStateChangedListener {
        private final WeakReference<TemplatePreviewActivity> activityWeakReference;

        public StateChangeListener(TemplatePreviewActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void onStateChanged(int newState) {
            TemplatePreviewActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.onStateChanged(newState);
            }
        }
    }

    private void onStateChanged(int newState) {
        mCurrentState = newState;
        if (newState == IPlayer.started) {
            mHasInit = true;
            mIvPlay.setVisibility(View.GONE);
            mIvCover.setVisibility(View.GONE);
            showAnimation(false);
        } else if (newState == IPlayer.paused) {
            mIvPlay.setVisibility(View.VISIBLE);
            showAnimation(false);
        } else if (newState == IPlayer.error) {
            showAnimation(false);
            ToastUtils.show(mContext, "网络不给力～");
        }
    }

    private void onPrepared() {
//        MediaInfo mediaInfo = mPlayer.getMediaInfo();
//        if (mediaInfo != null) {
//            List<TrackInfo> trackInfos = mediaInfo.getTrackInfos();
//            if (trackInfos != null) {
//                for (TrackInfo trackInfo : trackInfos) {
//                    Log.i(TAG, "vod play url " + trackInfo.getVodPlayUrl());
//                    Log.i(TAG, "file size " + trackInfo.getVodFileSize());
//                    Log.i(TAG, "vod definition " + trackInfo.getVodDefinition());
//                }
//            }
//        }
    }

    // 开始缓冲
    private void onLoadingBegin() {
        showAnimation(true);
    }

    // 缓冲结束
    private void onLoadingEnd() {
        showAnimation(false);
    }

    private void onLoadingProgress() {
        showAnimation(true);
    }


    private void onInfo(InfoBean infoBean) {
        if (infoBean.getCode() == InfoCode.CacheSuccess) {

        } else if (infoBean.getCode() == InfoCode.CacheError) {

        }
    }


    private void showAnimation(boolean show) {
        if (show) {
            if (mIvLoading.getVisibility() != View.VISIBLE) {
                mIvLoading.setVisibility(View.VISIBLE);
                AnimationDrawable frameAnimation = (AnimationDrawable) mIvLoading.getBackground();
                frameAnimation.start();
            }
        } else {
            if (mIvLoading.getVisibility() != View.GONE) {
                AnimationDrawable frameAnimation = (AnimationDrawable) mIvLoading.getBackground();
                frameAnimation.stop();
                mIvLoading.setVisibility(View.GONE);
            }
        }
    }

    private void pause() {
        if (mHasInit) {
            mPlayer.pause();
        }
    }

    private void resume() {
        if (mHasInit && mStartWhenOnResume) {
            mPlayer.start();
        }
    }

    private FollowVideoTemplateBean mFollowVideoTemplate;//模板数据

    private void initTemplateDownloadManager(Context context) {
        if (mDownloadManager == null) {
            mDownloadManager = PCFollowVideoSDK.createTemplateDownloadManager(context);
        }
    }

    //下载模板
    private void downloadTemplate() {
        if (!NetworkUtils.isNetworkAvailable(mContext)) {
            ToastUtils.show(mContext, "网络不给力哦");
            return;
        }
        initTemplateDownloadManager(mContext);
        mDownloadManager.download(mTemplateIno.getSourceUrl(), mTemplateIno.getId(), new TemplateDownloadManager.Callback() {
            @Override
            public void onFinish(FollowVideoTemplateBean templateBean) {
                if (hasDestroy() || templateBean == null) return;
                PCLog.onEvent(String.format(Locale.CHINA, "PreDownSuccess(id=%d)", mTemplateIno.getId()));
                mFollowVideoTemplate = templateBean;
                goToFjMainActivity(mFollowVideoTemplate);
                updateDownloadTemplateUi();
            }

            @Override
            public void onProgress(int percent) {

            }

            @Override
            public void onError(int status, String reason) {
                if (hasDestroy()) return;
                PCLog.onEvent(String.format(Locale.CHINA, "PreDownError(%s)", reason));
                ToastUtils.show(mContext, "下载失败，请重试");
            }
        });
    }

    //分镜首页
    private void goToFjMainActivity(FollowVideoTemplateBean stencilBean) {
        if (stencilBean != null) {
            PCFollowVideoSDK.jumpToFollowVideoMain(mContext, stencilBean, null);
//            FollowVideoFjMainActivity.start(mContext, stencilBean);
        }
    }

    //开启加载本地json任务
    private void startLoadStencil() {
        if (mTemplateIno.isPhotoType()) {
            return;
        }
        initTemplateDownloadManager(mContext);
        mDownloadManager.startLoadTemplateTask(mTemplateId, new TemplateDownloadManager.LoadTemplateCallback() {
            @Override
            public void onLoadTemplateComplete(FollowVideoTemplateBean templateBean) {
                if (hasDestroy()) return;
                mFollowVideoTemplate = templateBean;
                updateDownloadTemplateUi();
            }
        });
    }


    //是否需要下载
    private boolean isNeedDownload() {
        if (mFollowVideoTemplate != null
                && mTemplateIno != null
//                && mFollowVideoTemplate.getTemplateId() == mTemplateIno.getId()
                && mFollowVideoTemplate.getJsonUpdate() == mTemplateIno.getJsonUpdate()) {
            return false;
        } else {
            return true;
        }
    }

    //更新按钮状态
    private void updateDownloadTemplateUi() {
        if (mTemplateIno == null) return;
        if (mTemplateIno.isPhotoType()) {
            mTvDownloadTemplate.setText("跟我拍");
            return;
        }
        if (mFollowVideoTemplate == null) {
            mTvDownloadTemplate.setText("快来下载跟我拍模板");
        } else {
            mTvDownloadTemplate.setText("跟我拍");
        }
    }

    private void onDownloadTemplate() {
        PCLog.onEvent("PreClickOnTheDownload");
        if (mTemplateIno == null) return;
        if (mTemplateIno.isPhotoType()) {
            PCFollowVideoSDK.jumpToTakePhoto(mContext, mTemplateIno.getMaskUrl(), new TakePhotoCallback() {
                @Override
                public void onTakePhotoComplete(String photoPath) {
                    ToastUtils.show(mContext, "保存成功");
                    finish();
                }

                @Override
                public void onTakePhotoError(int code, String reason) {
                    ToastUtils.show(mContext, "拍照失败：" + reason);
                }
            });
            return;
        }
        if (isNeedDownload()) {
            if (TemplateUtil.canDownload(mContext, mTemplateIno == null ? "" : mTemplateIno.getAppVersion())) {
                downloadTemplate();
            } else {
                ToastUtils.show(mContext, "APP版本太低无法下载此模板");
            }
        } else {
            goToFjMainActivity(mFollowVideoTemplate);
        }
    }

    private ConnectivityManager connMgr;
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            if (hasDestroy()) return;
            if (mTemplateIno == null) {
                getData(mTemplateId);
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
        }
    };

}


