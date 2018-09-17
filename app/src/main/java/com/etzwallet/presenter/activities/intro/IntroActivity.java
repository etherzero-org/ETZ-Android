
package com.etzwallet.presenter.activities.intro;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.allenliu.versionchecklib.core.http.HttpRequestMethod;
import com.allenliu.versionchecklib.v2.AllenVersionChecker;
import com.allenliu.versionchecklib.v2.builder.UIData;
import com.allenliu.versionchecklib.v2.callback.RequestVersionListener;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.HomeActivity;
import com.etzwallet.presenter.activities.SetPinActivity;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.security.BRKeyStore;
import com.etzwallet.tools.security.PostAuth;
import com.etzwallet.tools.security.SmartValidator;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.util.JsonRpcHelper;
import com.platform.APIClient;
import com.tencent.bugly.crashreport.CrashReport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.io.Serializable;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip44.AddressIndex;
import io.github.novacrypto.bip44.BIP44;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class IntroActivity extends BRActivity implements Serializable {
    private static final String TAG = IntroActivity.class.getName();
    public Button newWalletButton;
    public Button recoverWalletButton;
    public static boolean appVisible = false;
    private static IntroActivity app;
    private View splashScreen;

    public static IntroActivity getApp() {
        return app;
    }

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashReport.initCrashReport(getApplicationContext(), "2f6ebc529a", false);

        setContentView(R.layout.activity_intro);
        newWalletButton = findViewById(R.id.button_new_wallet);
        recoverWalletButton = findViewById(R.id.button_recover_wallet);
        splashScreen = findViewById(R.id.splash_screen);
        setListeners();
        updateBundles();
//        ImageButton faq = findViewById(R.id.faq_button);

//        faq.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!BRAnimator.isClickAllowed()) return;
//                BaseWalletManager wm = WalletsMaster.getInstance(IntroActivity.this).getCurrentWallet(IntroActivity.this);
//                BRAnimator.showSupportFragment(IntroActivity.this, BRConstants.FAQ_START_VIEW, wm);
//            }
//        });

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        if (Utils.isEmulatorOrDebug(this)) {
            Utils.printPhoneSpecs();
        }

        byte[] masterPubKey = BRKeyStore.getMasterPublicKey(this);

        boolean isFirstAddressCorrect = false;
        if (masterPubKey != null && masterPubKey.length != 0) {
            isFirstAddressCorrect = SmartValidator.checkFirstAddress(this, masterPubKey);
        }
        if (!isFirstAddressCorrect) {
            WalletsMaster.getInstance(this).wipeWalletButKeystore(this);
        }

        PostAuth.getInstance().onCanaryCheck(this, false);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                splashScreen.setVisibility(View.GONE);
            }
        }, DateUtils.SECOND_IN_MILLIS);





    }




    private void updateBundles() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(IntroActivity.this);
                apiClient.updateBundle();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateBundle DONE in " + (endTime - startTime) + "ms");
            }
        });
    }

    private void setListeners() {
        newWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                HomeActivity bApp = HomeActivity.getApp();
                Intent intent = new Intent(IntroActivity.this, SetPinActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                if (bApp != null) bApp.finish();


//                generateKeyPair();
            }
        });

        recoverWalletButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                HomeActivity bApp = HomeActivity.getApp();
                if (bApp != null) bApp.finish();
                Intent intent = new Intent(IntroActivity.this, RecoverActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                if (bApp != null) bApp.finish();
            }
        });
    }

    /**
     * generate key pair to create eth wallet
     * 生成KeyPair , 用于创建钱包
     */
    public ECKeyPair generateKeyPair() {
        String TAG = "AAA";
        // 1. we just need eth wallet for now
        AddressIndex addressIndex = BIP44
                .m()
                .purpose44()
                .coinType(60)
                .account(0)
                .external()
                .address(0);
        // 2. calculate seed from mnemonics , then get master/root key ; Note that the bip39 passphrase we set "" for common
        String mn = "april cruise index artist wonder sell original cloth film sword actress own";
        ExtendedPrivateKey rootKey = ExtendedPrivateKey.fromSeed(new SeedCalculator().calculateSeed(mn, ""), Bitcoin.MAIN_NET);
//        Logger.i("generateKeyPair===mnemonics:" + mnemonics);
        String extendedBase58 = rootKey.extendedBase58();
//        Logger.i("generateKeyPair===extendedBase58:" + extendedBase58);

        // 3. get child private key deriving from master/root key
        ExtendedPrivateKey childPrivateKey = rootKey.derive(addressIndex, AddressIndex.DERIVATION);
        String childExtendedBase58 = childPrivateKey.extendedBase58();
//        Logger.i("generateKeyPair===childExtendedBase58:" + childExtendedBase58);

        // 4. get key pair
        byte[] privateKeyBytes = childPrivateKey.getKey();
        ECKeyPair keyPair = ECKeyPair.create(privateKeyBytes);

        // we 've gotten what we need
        String privateKey = childPrivateKey.getPrivateKey();
        String publicKey = childPrivateKey.neuter().getPublicKey();
        String address = Keys.getAddress(keyPair);

        Log.i(TAG,"generateKeyPair===privateKey:" + privateKey);
//        Logger.i("generateKeyPair===publicKey:" + publicKey);
        Log.i(TAG,"generateKeyPair===address:" + address);

        return keyPair;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
