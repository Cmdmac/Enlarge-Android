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
import android.widget.Toast;

/**
 * Created by fengzhiping on 2018/10/21.
 */

public class PermissionChecker {
    Activity mActivity;
    PermiisonSetting mSetting;
    Callback mCallback;
    public PermissionChecker(Activity activity, PermissionChecker.PermiisonSetting setting, Callback callback) {
        mActivity = activity;
        mSetting = setting;
        mCallback = callback;
    }
    public static final String[] PERMISSONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    public static class PermiisonSetting {
        public String permission;
        public String REQUEST_TITLE = "";
        public String REQUEST_MESSAGE = "";
        public String REQUEST_OPEN_SETTING_MESSAGE = "";
    }

    public static interface Callback {
        void onAllow();
        void onDeny();
    }

    public void check() {
        if (!hasPermission(mSetting.permission)) {
            showDialogTipUserRequestPermission();
        } else {
            if (mCallback != null) {
                mCallback.onAllow();
            }
        }
    }


    private boolean hasPermission(String permission){
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            int i = ContextCompat.checkSelfPermission(mActivity, permission);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                return false;
            }
        }
        return true;
    }

    private void showDialogTipUserRequestPermission() {
        new AlertDialog.Builder(mActivity)
                .setTitle(mSetting.REQUEST_TITLE)
                .setMessage(mSetting.REQUEST_MESSAGE)
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
                        Toast.makeText(mActivity, "用户取消授权", Toast.LENGTH_LONG).show();
                        if (mCallback != null) {
                            mCallback.onDeny();
                        }
//                        finish();
                    }
                }).setCancelable(false).show();
    }

    // 开始提交请求权限
    private void startRequestPermission() {
        ActivityCompat.requestPermissions(mActivity, new String[] {mSetting.permission}, 321);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 检查该权限是否已经获取
                int i = ContextCompat.checkSelfPermission(mActivity, mSetting.permission);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                    showDialogTipUserGoToAppSettting();
                } else {
                    if (mAlertDialog != null && mAlertDialog.isShowing()) {
                        mAlertDialog.dismiss();
                    }
                    Toast.makeText(mActivity, "权限获取成功", Toast.LENGTH_SHORT).show();
                    if (mCallback != null) {
                        mCallback.onAllow();
                    }
                }
            }
        }
    }

    AlertDialog mAlertDialog;
    // 提示用户去应用设置界面手动开启权限
    private void showDialogTipUserGoToAppSettting(){
        mAlertDialog = new AlertDialog.Builder(mActivity)
                .setTitle(mSetting.REQUEST_TITLE).setMessage(mSetting.REQUEST_OPEN_SETTING_MESSAGE)
                .setPositiveButton("立即开启",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 跳转到应用设置界面
                        goToAppSetting();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mActivity.finish();
                    }
                }).setCancelable(false).show();

    }

    // 跳转到当前应用的设置界面
    private void goToAppSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
        intent.setData(uri);
        mActivity.startActivityForResult(intent, 123);
    }
}
