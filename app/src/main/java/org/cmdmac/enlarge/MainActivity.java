package org.cmdmac.enlarge;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;

import org.cmdmac.enlarge.server.AppNanolets;
import org.cmdmac.enlarge.server.utils.Utils;
import org.cmdmac.enlarge.server.websocket.EnlargeWebSocket;
import org.cmdmac.enlargeserver.R;
import org.cmdmac.rx.Consumer;
import org.cmdmac.rx.Observable;
import org.cmdmac.rx.observable.ObservableEmitter;
import org.cmdmac.rx.observable.ObservableOnSubscribe;
import org.cmdmac.rx.scheduler.Schedulers;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity implements AppNanolets.PermissionEntries.OnPermissonChange {

    PermissionChecker mPermissionChecker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText("请打开http://cmdmac.xyz扫描二维码登录或用浏览器输入地址：" + Utils.getIPAddress(this) + ":" + AppNanolets.PORT);
        AppNanolets.start(this);

        AppNanolets.PermissionEntries.setPermissionChangeListener(this);


//        if (!hasPermission(permissions[1])) {
//            showDialogTipUserRequestPermission();
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPermissionChecker.onActivityResult(requestCode, resultCode, data);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                AppNanolets.start(this);
                break;
            case R.id.scan:
                PermissionChecker.PermiisonSetting setting = new PermissionChecker.PermiisonSetting();
                setting.permission = Manifest.permission.CAMERA;
                setting.REQUEST_TITLE = "拍照权限不可用";
                setting.REQUEST_MESSAGE = "Enlarge-Android需要拍照权限扫描二维码登录，是否允许开启拍照权限？";
                mPermissionChecker = new PermissionChecker(this, setting, new PermissionChecker.Callback() {
                    @Override
                    public void onAllow() {
                        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onDeny() {

                    }
                });
                mPermissionChecker.check();

                break;
        }
    }

    @Override
    public void onChange(String[] connectedList) {

        final StringBuilder sb = new StringBuilder();
        for (String item : connectedList) {
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
