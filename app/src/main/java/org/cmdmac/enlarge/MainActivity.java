package org.cmdmac.enlarge;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText("请打开http://cmdmac.xyz扫描二维码登录或用浏览器输入地址：" + Utils.getIPAddress(this) + ":" + AppNanolets.PORT);
        AppNanolets.start(this);

        AppNanolets.PermissionEntries.setPermissionChangeListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                break;
            case R.id.scan:
                Intent intent = new Intent(this, CaptureActivity.class);
                startActivity(intent);
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
