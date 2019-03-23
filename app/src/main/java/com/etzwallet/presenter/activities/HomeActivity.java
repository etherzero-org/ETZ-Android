package com.etzwallet.presenter.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import com.allenliu.versionchecklib.core.http.HttpRequestMethod;
import com.allenliu.versionchecklib.v2.AllenVersionChecker;
import com.allenliu.versionchecklib.v2.builder.UIData;
import com.allenliu.versionchecklib.v2.callback.RequestVersionListener;
import com.etzwallet.R;
import com.etzwallet.core.BRCoreKey;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.fragments.FragmentDiscovery;
import com.etzwallet.presenter.fragments.FragmentMy;
import com.etzwallet.presenter.fragments.FragmentWallet;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.exceptions.UserNotAuthenticatedException;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.security.BRKeyStore;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.util.JsonRpcHelper;

import org.json.JSONObject;

public class HomeActivity extends BRActivity implements OnClickListener {
    private String[] mFragmentTagList = {"FragmentWallet", "FragmentDiscovery", "FragmentMy"};
    private Fragment mCurrentFragmen = null; // 记录当前显示的Fragment
    private FragmentManager mFm;
    private LinearLayout main_daohanglan;

    protected static HomeActivity mActivity = null;

    public static HomeActivity getApp() {
        return mActivity;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fg = new Fragment[3];
        main_daohanglan = findViewById(R.id.main_daohanglan);
        mFm = getSupportFragmentManager();
        mCurrentFragmen = createFragment(0);
        mActivity = this;
        findViewById(R.id.main_ll_wallet).setOnClickListener(this);
        findViewById(R.id.main_ll_discivery).setOnClickListener(this);
        findViewById(R.id.main_ll_my).setOnClickListener(this);
        isClickLL(true, false, false);
        showFagment(0);
        //检查新版本
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                alertDialog();
                checkVersionUpdate();
            }
        });
//        BRCoreKey key= null;
//        try {
//            key = new BRCoreKey( new String(BRKeyStore.getPhrase(getApplicationContext(), 0)));
//            String pkey=key.getPrivKey();
//            MyLog.i("--------pkey="+pkey);
//        } catch (UserNotAuthenticatedException e) {
//            e.printStackTrace();
//        }


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_ll_wallet:
                //实现状态栏图标和文字颜色为暗色
//                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                isClickLL(true, false, false);
                showFagment(0);
                break;
            case R.id.main_ll_discivery:
                isClickLL(false, true, false);
                showFagment(1);
                break;
            case R.id.main_ll_my:
                //实现状态栏图标和文字颜色为浅色
//                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                isClickLL(false, false, true);
                showFagment(2);
                break;

        }
    }

    public void isClickLL(boolean tab01, boolean tab02, boolean tab03) {
        findViewById(R.id.main_ll_wallet).setSelected(tab01);
        findViewById(R.id.main_ll_discivery).setSelected(tab02);
        findViewById(R.id.main_ll_my).setSelected(tab03);
    }

    private Fragment[] fg;

    // 转换Fragment
    public void showFagment(int index) {
        if (fg[index] == null) {
            fg[index] = createFragment(index);
        }

        if (mCurrentFragmen != fg[index]) {
            FragmentTransaction transaction = mFm.beginTransaction();
            if (!fg[index].isAdded()) {
                // 没有添加过:
                // 隐藏当前的，添加新的，显示新的
                fg[index] = createFragment(index);
                transaction
                        .hide(mCurrentFragmen)
                        .add(R.id.main_fl_info, fg[index],
                                mFragmentTagList[index]).show(fg[index]);
            } else {
                // 隐藏当前的，显示新的
                transaction.hide(mCurrentFragmen).show(fg[index]);
            }
            mCurrentFragmen = fg[index];
            transaction.commitAllowingStateLoss();

        }
    }


    private Fragment createFragment(int index) {
        switch (index) {
            case 0:
                return new FragmentWallet();
            case 1:
                return new FragmentDiscovery();
            case 2:
                return new FragmentMy();

        }
        return null;
    }

    public void isShowNavigationBar(boolean isShow) {
        if (isShow) {
            main_daohanglan.setVisibility(View.VISIBLE);
        } else {
            main_daohanglan.setVisibility(View.GONE);
        }
    }

    public void checkVersionUpdate() {
        Context ctx = this.getApplicationContext();
        AllenVersionChecker
                .getInstance()
                .requestVersion()
                .setRequestMethod(HttpRequestMethod.GET)
                .setRequestUrl(JsonRpcHelper.versionCheekUrl())
                .request(new RequestVersionListener() {
                    @Nullable
                    @Override
                    public UIData onRequestVersionSuccess(String result) {

                        try {
                            if (Utils.isNullOrEmpty(result)) {
                                MyLog.i("onRequestVersionSuccess: 获取新版本失败1");
                            }
                            JSONObject json = new JSONObject(result);
                            MyLog.i("onRequestVersionSuccess: json==" + json);
                            JSONObject json1 = new JSONObject(json.getString("result"));

                            String dlUrl = json1.getString("url");
                            String dlContent = json1.getString("content");
                            String versionCode = json1.getString("versionCode");
                            String versionName = json1.getString("version");

                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(getString(R.string.current_version));
                            stringBuilder.append(versionName);
                            stringBuilder.append("\n");
                            stringBuilder.append(getString(R.string.update_content));
                            stringBuilder.append(dlContent);

                            String finalString = stringBuilder.toString();

                            if (Integer.parseInt(versionCode) > Integer.parseInt(getVersionCode())) {
                                UIData uiData = UIData
                                        .create()
                                        .setDownloadUrl(dlUrl)
                                        .setTitle(getString(R.string.download_latest_version))
                                        .setContent(finalString);
                                return uiData;
                            } else {
                                return null;
                            }

                        } catch (Exception e) {
                            MyLog.i("onRequestVersionSuccess: 获取新版本失败2");
                        }

                        return null;
                    }

                    @Override
                    public void onRequestVersionFailure(String message) {

                    }
                })
                .excuteMission(ctx);

    }

    public String getVersionCode() {
        Context ctx = this.getApplicationContext();
        PackageManager packageManager = ctx.getPackageManager();
        PackageInfo packageInfo;
        String versionCode = "";
        try {
            packageInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            versionCode = packageInfo.versionCode + "";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    private void alertDialog() {
        Boolean addErr = BRSharedPrefs.getAddressError(mActivity);
        MyLog.i("onRecoverWalletAuth: adderr===" + addErr);
        if (!addErr) {
            BRDialog.showCustomDialog(mActivity, getString(R.string.Alert_error),
                    getString(R.string.Alert_keystore_generic_android_bug),
                    getString(R.string.Button_ok),
                    null,
                    new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            finish();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    }, 0);
            BRSharedPrefs.putAddressError(mActivity, false);
        }
    }

    public void onResume() {
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void onBackPressed() {
        moveTaskToBack(true);
    }

    // 当activity非正常销毁时被调用
    @SuppressLint("NewApi")
    @Override
    public void onSaveInstanceState(Bundle outState,
                                    PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        // 重置Fragment，防止当内存不足时导致Fragment重叠
        updateFragment(outState);
    }

    // 重置Fragment
    private void updateFragment(Bundle outState) {

        mFm = getSupportFragmentManager();
        if (outState == null) {
            FragmentTransaction transaction = mFm.beginTransaction();
            FragmentWallet walletFragment = new FragmentWallet();
            mCurrentFragmen = walletFragment;
            transaction.add(R.id.main_fl_info, walletFragment,
                    mFragmentTagList[0]).commitAllowingStateLoss();
        } else {
            // 通过tag找到fragment并重置
            FragmentWallet oneFragment = (FragmentWallet) mFm
                    .findFragmentByTag(mFragmentTagList[0]);
            FragmentDiscovery twoFragment = (FragmentDiscovery) mFm
                    .findFragmentByTag(mFragmentTagList[1]);
            FragmentMy threeFragment = (FragmentMy) mFm
                    .findFragmentByTag(mFragmentTagList[2]);
            mFm.beginTransaction().show(oneFragment).hide(twoFragment)
                    .hide(threeFragment);
        }
    }

}
