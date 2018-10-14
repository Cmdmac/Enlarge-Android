package org.cmdmac.enlarge;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.cmdmac.enlarge.server.AppNanolets;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText(getIPAddress(this));
    }

    private static class PermissionProcesser implements AppNanolets.RemoteConnectListener {

        private boolean isRequestingPermission = false;
        private Context mContext;

        public PermissionProcesser(Context context) {
            mContext = context;
        }

        @Override
        public boolean isConnectAllow(String uri) {
            if (isRequestingPermission) {
                return AppNanolets.isEnableRemoteConnect();
            } else {
                isRequestingPermission = true;
                try {
                    Observable.create(new ObservableOnSubscribe<Boolean>() {
                        @Override
                        public void subscribe(final ObservableEmitter<Boolean> observableEmitter) {
                            new AlertDialog.Builder(mContext).setPositiveButton("允许", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(PermissionProcesser.class.getSimpleName(), "allow");
                                    observableEmitter.onNext(true);
                                }
                            }).setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(PermissionProcesser.class.getSimpleName(), "deny");
                                    observableEmitter.onNext(false);
                                }
                            }).setTitle("提示").create().show();
                        }
                    }).subscribeOn(Schedulers.mainThread()).observeOn(Schedulers.mainThread()).subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) {
                            if (aBoolean) {
                                AppNanolets.enableRemoteConnect();
                            }
                            synchronized (PermissionProcesser.this) {
                                PermissionProcesser.this.notifyAll();
                            }
                        }
                    });

                    synchronized (this) {
                        this.wait();
                    }
                    Log.e(PermissionProcesser.class.getSimpleName(), "after wait");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return AppNanolets.isEnableRemoteConnect();
            }
        }
    }

    public void onClick(View v) {
        AppNanolets.start(new PermissionProcesser(this));
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ipAddress = intIP2StringIP(wifiInfo.getIpAddress());//得到IPV4地址
                return ipAddress;
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null;
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }
}
