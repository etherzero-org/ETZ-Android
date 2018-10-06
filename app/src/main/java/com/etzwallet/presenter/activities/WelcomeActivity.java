package com.etzwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;

import com.etzwallet.presenter.activities.intro.IntroActivity;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.tools.manager.BRSharedPrefs;


public class WelcomeActivity extends BRActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BRSharedPrefs.getFirstAddress(this) != null && !BRSharedPrefs.getFirstAddress(this).equals("")) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, IntroActivity.class));
        }
        finish();
    }
}
