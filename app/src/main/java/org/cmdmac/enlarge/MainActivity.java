package org.cmdmac.enlarge;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.client.android.CaptureActivity;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.eventbus.PermissionResultEvent;
import org.cmdmac.enlarge.server.eventbus.ConnectedChangeEvent;
import org.cmdmac.enlarge.server.utils.Utils;
import org.cmdmac.enlargeserver.R;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText("请打开http://cmdmac.xyz扫描二维码登录或用浏览器输入地址：" + Utils.getIPAddress(this) + ":" + AppNanolets.PORT);
        AppNanolets.start(this);

        EventBus.getDefault().register(this);

        new PermissionChecker.Builder(this).permission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .requestTitle("存储权限不可用")
                .requestMessage("Enlarge-Android需要存储权限操作存储设备，是否允许开启存储权限？")
                .callback(new PermissionChecker.Callback() {
                    @Override
                    public void onAllow() {
                    }

                    @Override
                    public void onDeny() {

                    }
                }).build().check();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EventBus.getDefault().post(new PermissionResultEvent(requestCode, permissions, grantResults));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                AppNanolets.start(this);
                break;
            case R.id.scan:

                new PermissionChecker.Builder(this)
                        .permission(Manifest.permission.CAMERA)
                        .requestTitle("拍照权限不可用")
                        .requestMessage("Enlarge-Android需要拍照权限扫描二维码登录，是否允许开启拍照权限？")
                        .callback(new PermissionChecker.Callback() {
                            @Override
                            public void onAllow() {
                                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                                startActivity(intent);
                            }

                            @Override
                            public void onDeny() {

                            }
                        }).build().check();

                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChange(ConnectedChangeEvent event) {

        final StringBuilder sb = new StringBuilder();
        for (String item : event.remotes) {
            sb.append(item).append("\n");
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView)findViewById(R.id.connected_list);
                textView.setText(sb);
            }
        });

    }


}
