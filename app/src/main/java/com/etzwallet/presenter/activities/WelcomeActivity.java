package com.etzwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.intro.IntroActivity;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.security.BRKeyStore;
import com.etzwallet.tools.util.Utils;


public class WelcomeActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        new TimeCount(1500,100).start();
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


    class TimeCount extends CountDownTimer
    {
        public TimeCount(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);// 参数依次为总时长,和计时的时间间隔
        }

        @Override
        public void onFinish()
        {// 计时完毕时触发
            String byt = BRKeyStore.getPinCode(BreadApp.getBreadContext());
            if (BRSharedPrefs.getIsSetPinCode(BreadApp.getBreadContext()) || !Utils.isNullOrEmpty(byt)) {
                MyLog.i("WelcomeActivity---有pin码");
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            } else {
                MyLog.i("WelcomeActivity---无pin码");
                startActivity(new Intent(WelcomeActivity.this, IntroActivity.class));
            }
            finish();

        }

        @Override
        public void onTick(long millisUntilFinished)
        {// 计时过程显示

        }
    }
}
