package cn.com.pcauto.video.uilibrary.widget;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import cn.com.pcauto.video.library.bumptech.glide.Glide;
import cn.com.pcauto.video.uilibrary.R;

/**
 * Created by 0 on 2017/8/2.
 */

public class StatusView extends FrameLayout {
    private View mLoadingLayout;
    private ImageView mLoadingIv;
    private TextView loadingTextTv;
    private View mErrorLayout;
    private TextView mTipTv;
    private ImageView mTipIv;
    private TextView mReload;

    private String mNetworkHint;
    private String mNoDataHint;
    private View mNoVideoLayout;

    private StatusListener mListener;
    private int type;
    private ImageView mVideoTipIv;
    private TextView mVideoTipTv;
    private TextView mVideoTv;
//    private LinearLayout mLlLoadingProgressBar;

    public StatusView(Context context) {
        super(context);
        init(context);
    }

    public StatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mNoDataHint = getResources().getString(R.string.pc_lib_video_no_data_hint);
        mNetworkHint = getResources().getString(R.string.pc_lib_video_network_fail_retry_hint);

        setEnabled(false);
        setClickable(true);

        LayoutInflater.from(context).inflate(R.layout.pc_lib_video_status_view, this);
        setBackgroundColor(Color.parseColor("#f0f2f7"));
        setVisibility(GONE);

        mLoadingLayout = findViewById(R.id.loading);
        mLoadingIv = (ImageView) findViewById(R.id.loading_iv);
        loadingTextTv = findViewById(R.id.loading_tv);
        mErrorLayout = findViewById(R.id.error_layout);
        mNoVideoLayout = findViewById(R.id.novideo_layout);
        mTipIv = (ImageView) findViewById(R.id.tip_img);
        mVideoTipIv = (ImageView) findViewById(R.id.no_videodata_img);
        mTipTv = (TextView) findViewById(R.id.tip);
        mVideoTipTv = (TextView) findViewById(R.id.no_videodata_tip);
        mReload = (TextView) findViewById(R.id.reload);
        mVideoTv = (TextView) findViewById(R.id.video_btn);
//        mLlLoadingProgressBar = (LinearLayout) findViewById(R.id.ll_new_loading_progress);
        Glide.with(context).load(R.drawable.pc_lib_video_loading_gif).into(mLoadingIv);
    }


    public void setColor(String color) {
        setBackgroundColor(Color.parseColor(color));
    }

    public void setColor(int color) {
        setBackgroundColor(color);
    }




    public StatusView setNoDataHint(String hint) {
        mNoDataHint = hint;
        return this;
    }

    public StatusView setNetWorkHint(String hint) {
        mNetworkHint = hint;
        return this;
    }

    public StatusView setListener(StatusListener listener) {
        mListener = listener;
        return this;
    }

    /**
     * 显示文字，ProgressBar在转动
     */
    public void loading() {
        setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(VISIBLE);
        mErrorLayout.setVisibility(GONE);
    }

//    /**
//     * 新版样式，旋转的菊花
//     */
//    public void loadingWithProgressBar() {
//        setVisibility(VISIBLE);
//        mLlLoadingProgressBar.setVisibility(VISIBLE);
//        mErrorLayout.setVisibility(GONE);
//    }

    public void loaded() {
        setVisibility(GONE);
    }

    public void networkError() {
        internalNetworkError(true);
    }

    public void networkErrorWithoutTip() {
        internalNetworkError(false);
    }

    private void internalNetworkError(boolean showTip) {
        setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(GONE);
//        mLlLoadingProgressBar.setVisibility(GONE);
        mErrorLayout.setVisibility(VISIBLE);
        mReload.setVisibility(VISIBLE);
        mReload.setText("重新加载");
        mTipTv.setText(mNetworkHint);
        if (showTip) {
            mTipIv.setVisibility(VISIBLE);
            mTipIv.setImageResource(R.drawable.pc_lib_video_error);
        } else {
            mTipIv.setVisibility(GONE);
        }
        mReload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onRetry();
                }
            }
        });
    }


    public void noData() {
        setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(GONE);
//        mLlLoadingProgressBar.setVisibility(GONE);
        mErrorLayout.setVisibility(VISIBLE);
        mTipTv.setText(mNoDataHint);
        mReload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onNoDataBtnClick();
                }
            }
        });
    }

    public void noData(int res, String hint) {
        noData(res, hint, null);
    }

    public void noData(int res, String hint, String btnText) {
        setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(GONE);
//        mLlLoadingProgressBar.setVisibility(GONE);
        mErrorLayout.setVisibility(VISIBLE);
        mTipTv.setText(hint);
        mTipIv.setImageResource(res);
        mReload.setText(btnText);
        if (TextUtils.isEmpty(btnText)) mReload.setVisibility(GONE);
        mReload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onNoDataBtnClick();
                }
            }
        });
    }

    public void noVideoData(int res, String hint, String btnText) {
        setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(GONE);
//        mLlLoadingProgressBar.setVisibility(GONE);
        mNoVideoLayout.setVisibility(VISIBLE);
        mErrorLayout.setVisibility(GONE);
        mVideoTipTv.setText(hint);
        mVideoTipIv.setImageResource(res);
        mVideoTv.setText(btnText);
        if (TextUtils.isEmpty(btnText)) mVideoTv.setVisibility(GONE);
        mVideoTv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onVideoBtnClick();
                }
            }
        });
    }

    public void videoTextDisplay(boolean visible) {
        if (null != mVideoTv) {
            mVideoTv.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }



    public static class StatusListener {

        public void onRetry() {
        }

        public void onNoDataBtnClick() {
        }

        public void onVideoBtnClick() {
        }
    }

//    public boolean dispatchTouchEvent(MotionEvent ev) {
//       Log.e("sunzn", "TouchEventFather | dispatchTouchEvent --> " + TouchEventUtil.getTouchAction(ev.getAction()));
////        return super.dispatchTouchEvent(ev);
//         return false;
//    }
//
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        Log.i("sunzn", "TouchEventFather | onInterceptTouchEvent --> " + TouchEventUtil.getTouchAction(ev.getAction()));
//        return super.onInterceptTouchEvent(ev);
//        // return false;
//    }
//
//    public boolean onTouchEvent(MotionEvent ev) {
//        Log.d("sunzn", "TouchEventFather | onTouchEvent --> " + TouchEventUtil.getTouchAction(ev.getAction()));
//        return super.onTouchEvent(ev);
//    }

}
