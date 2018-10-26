package com.etzwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.etzwallet.R;
import com.etzwallet.presenter.activities.intro.IntroActivity;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.security.BRKeyStore;


public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        if (BRKeyStore.getPinCode(this) != null && !BRKeyStore.getPinCode(this).equals("")) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            MyLog.i(BRSharedPrefs.getFirstAddress(this));
            startActivity(new Intent(this, IntroActivity.class));
        }
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
