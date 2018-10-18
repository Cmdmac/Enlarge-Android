package org.cmdmac.enlarge;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.cmdmac.enlarge.server.utils.Utils;
import org.cmdmac.enlargeserver.R;
import org.json.JSONException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    String mUrl;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        Intent intent = getIntent();
        mUrl = intent.getStringExtra("url");
        setTitle("登录");
    }

    private void onLogin() {
        if (TextUtils.isEmpty(mUrl)) {
            return;
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        final Request request = new Request.Builder()
                .url(mUrl + "http://" + Utils.getIPAddress(this))
                .get()//默认就是GET请求，可以不写
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(LoginActivity.class.getSimpleName(), "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(LoginActivity.class.getSimpleName(), "onResponse: " + response.body().string());
                try {
                    org.json.JSONObject j = new org.json.JSONObject(response.body().string());
                    if (j.has("code")) {
                        String code = j.getString("code");
                        if (!TextUtils.isEmpty(code)) {
                            if (code.equals("ok")) {
                                finish();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:
                onLogin();
                break;

            case R.id.cancel:
                finish();
                break;
        }
    }
}
