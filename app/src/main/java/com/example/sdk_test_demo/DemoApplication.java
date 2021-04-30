package com.example.sdk_test_demo;

import android.app.Application;

import cn.com.pcauto.video.library.PCFollowVideoConfig;
import cn.com.pcauto.video.library.PCFollowVideoSDK;
import cn.com.pcauto.video.library.PCSDKCallback;
import cn.com.pcauto.video.library.utils.ToastUtils;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PCFollowVideoSDK.init(getApplicationContext(), new PCFollowVideoConfig("448df650cdb7833ff3667fe70ad17145", "4412201"), new PCSDKCallback() {
            @Override
            public void onRegisterError(String code, String message) {
                if ("400".equals(code)) {
                    ToastUtils.show(DemoApplication.this, "key失效，请联系客服");
                } else {
                    ToastUtils.show(DemoApplication.this, message);
                }
            }

            @Override
            public void onRegisterSuccess() {

            }
        });
    }
}
