package org.cmdmac.enlarge;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.cmdmac.enlargeserver.R;

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

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:

                break;

            case R.id.cancel:
                finish();
                break;
        }
    }
}
