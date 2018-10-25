package org.cmdmac.enlarge;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.cmdmac.enlarge.server.eventbus.ActivityResultEvent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by fengzhiping on 2018/10/21.
 */

public class PermissionChecker {
    Builder builder;

    private PermissionChecker(Builder builder) {
        this.builder = builder;
        EventBus.getDefault().register(this);
    }

    public static class Builder {
        Activity activity;
        String permission;
        String requestTitle = "";
        String requestMessage = "";
        String requestOpenSettingMessage = "";
        Callback callback;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        Builder requestTitle(String title) {
            this.requestTitle = title;
            return this;
        }

        Builder requestMessage(String message) {
            this.requestMessage = message;
            return this;
        }

        Builder requestOpenSettingMessage(String requestOpenSettingMessage) {
            this.requestOpenSettingMessage = requestOpenSettingMessage;
            return this;
        }

        Builder callback(Callback callback) {
            this.callback = callback;
            return this;
        }

        PermissionChecker build() {
            return new PermissionChecker(this);
        }

    }

    public static final String[] PERMISSONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    public static interface Callback {
        void onAllow();
        void onDeny();
    }

    public void check() {
        if (!hasPermission(builder.permission)) {
            showDialogTipUserRequestPermission();
        } else {
            if (builder.callback != null) {
                builder.callback.onAllow();
            }
        }
    }


    private boolean hasPermission(String permission){
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            int i = ContextCompat.checkSelfPermission(builder.activity, permission);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                return false;
            }
        }
        return true;
    }

    private void showDialogTipUserRequestPermission() {
        new AlertDialog.Builder(builder.activity)
                .setTitle(builder.requestTitle)
                .setMessage(builder.requestMessage)
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(builder.activity, "用户取消授权", Toast.LENGTH_LONG).show();
                        if (builder.callback != null) {
                            builder.callback.onDeny();
                        }
//                        finish();
                    }
                }).setCancelable(false).show();
    }

    // 开始提交请求权限
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(builder.activity, new String[] {builder.permission}, 321);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActivityResult(ActivityResultEvent event) {
        Log.e("DDD", "on");
        onActivityResult(event.requestCode, event.resultCode, event.data);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 检查该权限是否已经获取
                int i = ContextCompat.checkSelfPermission(builder.activity, builder.permission);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                    showDialogTipUserGoToAppSettting();
                } else {
                    if (mAlertDialog != null && mAlertDialog.isShowing()) {
                        mAlertDialog.dismiss();
                    }
                    Toast.makeText(builder.activity, "权限获取成功", Toast.LENGTH_SHORT).show();
                    if (builder.callback != null) {
                        builder.callback.onAllow();
                    }
                }
            }
        }
    }

    AlertDialog mAlertDialog;
    // 提示用户去应用设置界面手动开启权限
    private void showDialogTipUserGoToAppSettting(){
        mAlertDialog = new AlertDialog.Builder(builder.activity)
                .setTitle(builder.requestTitle).setMessage(builder.requestOpenSettingMessage)
                .setPositiveButton("立即开启",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        builder.activity.finish();
                    }
                }).setCancelable(false).show();

    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", builder.activity.getPackageName(), null);
        intent.setData(uri);
        builder.activity.startActivityForResult(intent, 123);
    }
}
