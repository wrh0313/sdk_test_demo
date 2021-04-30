package cn.com.pcauto.video.uilibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

@Deprecated
public class PhotoInfoActivity extends AppCompatActivity {

    public static void start(Activity activity, String path) {
        if (activity != null) {
            activity.startActivity(new Intent(activity, PhotoInfoActivity.class).putExtra("path", path));
        }
    }

    String path;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        path = getIntent().getStringExtra("path");
        TextView textView = new TextView(this);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(textView);

        textView.setText(path);
        textView.setTextSize(19);
        textView.setGravity(Gravity.CENTER);

    }
}
