package com.etzwallet.wallet;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.WorkerThread;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.core.BRCoreKey;
import com.etzwallet.core.BRCoreMasterPubKey;
import com.etzwallet.core.ethereum.BREthereumToken;
import com.etzwallet.core.ethereum.BREthereumWallet;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.exceptions.UserNotAuthenticatedException;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.security.BRKeyStore;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.tools.util.Bip39Reader;
import com.etzwallet.tools.util.TrustedNode;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.etzwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;
import com.etzwallet.wallet.wallets.ethereum.WalletTokenManager;
import com.platform.entities.TokenListMetaData;
import com.platform.entities.WalletInfo;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
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

public class WalletsMaster {
    private static final String TAG = WalletsMaster.class.getName();

    private static WalletsMaster instance;

    private List<BaseWalletManager> mWallets = new ArrayList<>();
    private TokenListMetaData mTokenListMetaData;

    private WalletsMaster(Context app) {
    }

    public synchronized static WalletsMaster getInstance(Context app) {
        if (instance == null) {
            instance = new WalletsMaster(app);
        }
        return instance;
    }

    //expensive operation (uses the KVStore), only update when needed and not in a loop.
    public synchronized void updateWallets(Context app) {
        WalletEthManager ethWallet = WalletEthManager.getInstance(app);
        if (ethWallet == null) {
            return; //return empty wallet list if ETH is null (meaning no public key yet)
        }

        mWallets.clear();
        mTokenListMetaData = KVStoreManager.getInstance().getTokenListMetaData(app);
        if (mTokenListMetaData == null) {
            List<TokenListMetaData.TokenInfo> enabled = new ArrayList<>();
            enabled.add(new TokenListMetaData.TokenInfo("BTC", false, null));
            enabled.add(new TokenListMetaData.TokenInfo("ETZ", false, null));
            //添加token钱包
            enabled.add(new TokenListMetaData.TokenInfo("EASH", true, "0x013b6e279989aa20819a623630fe678c9f43a48f"));
            mTokenListMetaData = new TokenListMetaData(enabled, null);
            KVStoreManager.getInstance().putTokenListMetaData(app, mTokenListMetaData); //put default currencies if null
        }
//        else {
//            boolean istrue=false;
//            for (TokenListMetaData.TokenInfo enabled : mTokenListMetaData.enabledCurrencies) {
//                if (enabled.symbol.equalsIgnoreCase("EASH") ) {
//                    istrue=true;
//                }
//            }
//            if (!istrue){
//                //添加token钱包
//                mTokenListMetaData.enabledCurrencies.add(new TokenListMetaData.TokenInfo("EASH", true, "0x013b6e279989aa20819a623630fe678c9f43a48f"));
//                KVStoreManager.getInstance().putTokenListMetaData(app, mTokenListMetaData); //put default currencies if null
//            }
//        }

        for (TokenListMetaData.TokenInfo enabled : mTokenListMetaData.enabledCurrencies) {

            boolean isHidden = mTokenListMetaData.isCurrencyHidden(enabled.symbol);

            if (enabled.symbol.equalsIgnoreCase("BTC") && !isHidden) {
                //BTC wallet
                mWallets.add(WalletBitcoinManager.getInstance(app));
            }  else if (enabled.symbol.equalsIgnoreCase("ETZ") && !isHidden) {
                //ETH wallet
                mWallets.add(ethWallet);
            }  else {
                //其他代币
                MyLog.i( "updateWallets: enabled.symbol222===" + enabled.symbol);
                WalletTokenManager tokenWallet = WalletTokenManager.getTokenWalletByIso(app, ethWallet, enabled.symbol);
                if (tokenWallet != null && !isHidden) mWallets.add(tokenWallet);

            }

        }
    }

    public synchronized List<BaseWalletManager> getAllWallets(Context app) {
        if (mWallets == null || mWallets.size() == 0) {
            updateWallets(app);
        }
        return mWallets;

    }

    //return the needed wallet for the iso
    public BaseWalletManager getWalletByIso(Context app, String iso) {

        if (iso == "ETH") {
            iso = "ETZ";
            MyLog.i( "getWalletByIso: iso==" + iso);
            MyLog.i( "getWalletByIso: +++" + iso.equalsIgnoreCase("ETZ"));
        }
        if (Utils.isNullOrEmpty(iso)) {
            throw new RuntimeException("getWalletByIso with iso = null, Cannot happen!");
        }else if (iso.equalsIgnoreCase("BTC")) {
            return WalletBitcoinManager.getInstance(app);
        }else  if (iso.equalsIgnoreCase("ETZ")) {
            return WalletEthManager.getInstance(app);
        }  else if (isIsoErc20(app, iso)) {
            return WalletTokenManager.getTokenWalletByIso(app, WalletEthManager.getInstance(app), iso);
        }
        return null;
    }

    public BaseWalletManager getCurrentWallet(Context app) {
        return getWalletByIso(app, BRSharedPrefs.getCurrentWalletIso(app));
    }

    //get the total fiat balance held in all the wallets in the smallest unit (e.g. cents)
    public BigDecimal getAggregatedFiatBalance(Context app) {
        long start = System.currentTimeMillis();
        BigDecimal totalBalance = BigDecimal.ZERO;
        List<BaseWalletManager> list = new ArrayList<>(getAllWallets(app));
        for (BaseWalletManager wallet : list) {
            BigDecimal fiatBalance = wallet.getFiatBalance(app);
            if (fiatBalance != null)
                totalBalance = totalBalance.add(fiatBalance);
        }
        return totalBalance;
    }

    //创建钱包生成种子
    public synchronized boolean generateRandomSeed(final Context ctx) {
        SecureRandom sr = new SecureRandom();
        final String[] words;
        List<String> list;
        String languageCode = Locale.getDefault().getLanguage();
        MyLog.i("glanguageCode===" + ctx.getResources().getConfiguration().locale.getCountry());
        if (languageCode == null) languageCode = "en";
        if (ctx.getResources().getConfiguration().locale.getCountry().equals("TW")) {
            list = Bip39Reader.bip39List(ctx, "zhTW");
        } else {
            list = Bip39Reader.bip39List(ctx, languageCode);
        }

        MyLog.i( "generateRandomSeed: list===" + list);
        words = list.toArray(new String[list.size()]);
//        final String[] words1 = list.toArray(new String[20]);
        MyLog.i( "generateRandomSeed: words===" + words);//設置完pin密碼生成一次  輸完lock 密碼生成一次
//        MyLog.i( "generateRandomSeed: words1===" + words1);

        final byte[] randomSeed = sr.generateSeed(16);
        if (words.length != 2048) {
            BRReportsManager.reportBug(new IllegalArgumentException("the list is wrong, size: " + words.length), true);
            return false;
        }
        MyLog.i("randomSeed.length=" + randomSeed.length);//
        if (randomSeed.length != 16)
            throw new NullPointerException("failed to create the seed, seed length is not 128: " + randomSeed.length);
        byte[] paperKeyBytes = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);
        if (paperKeyBytes == null || paperKeyBytes.length == 0) {
            BRReportsManager.reportBug(new NullPointerException("failed to encodeSeed"), true);
            return false;
        }
        String[] splitPhrase = new String(paperKeyBytes).split(" ");
        MyLog.i( "splitPhrase.length=" + splitPhrase.length);//
        if (splitPhrase.length != 12) {
            BRReportsManager.reportBug(new NullPointerException("phrase does not have 12 words:" + splitPhrase.length + ", lang: " + languageCode), true);
            return false;
        }
        boolean success = false;
        try {
            success = BRKeyStore.putPhrase(paperKeyBytes, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return false;
        }
        MyLog.i( "success=" + success);//
        if (!success) return false;
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(ctx, 0);

        } catch (UserNotAuthenticatedException e) {
            throw new RuntimeException("Failed to retrieve the phrase even though at this point the system auth was asked for sure.");
        }
        if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("phrase is null!!");
        if (phrase.length == 0) throw new RuntimeException("phrase is empty");
        MyLog.d( "generateRandomSeed: phrase===" + phrase);
        byte[] seed = BRCoreKey.getSeedFromPhrase(phrase);
        if (seed == null || seed.length == 0) throw new RuntimeException("seed is null");
        byte[] authKey = BRCoreKey.getAuthPrivKeyForAPI(seed);
        if (authKey == null || authKey.length == 0) {
            BRReportsManager.reportBug(new IllegalArgumentException("authKey is invalid"), true);
        }
        BRKeyStore.putAuthKey(authKey, ctx);
        int walletCreationTime = (int) (System.currentTimeMillis() / 1000);
        BRKeyStore.putWalletCreationTime(walletCreationTime, ctx);
        final WalletInfo info = new WalletInfo();
        info.creationDate = walletCreationTime;
        KVStoreManager.getInstance().putWalletInfo(ctx, info); //push the creation time to the kv store

        //store the serialized in the KeyStore
        byte[] pubKey = new BRCoreMasterPubKey(paperKeyBytes, true).serialize();
        BRKeyStore.putMasterPublicKey(pubKey, ctx);

        return true;

    }

    public boolean isIsoCrypto(Context app, String iso) {
//        MyLog.i( "isIsoCrypto: ios===="+iso);
        List<BaseWalletManager> list = new ArrayList<>(getAllWallets(app));
        for (BaseWalletManager w : list) {

            if (w.getIso().equalsIgnoreCase(iso)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIsoErc20(Context app, String iso) {
        if (Utils.isNullOrEmpty(iso)) return false;
        try {
            BREthereumToken[] tokens = WalletEthManager.getInstance(app).node.tokens;
            for (BREthereumToken token : tokens) {
                if ((token!=null&&!Utils.isNullOrEmpty(token.getSymbol()))&&iso.equals(token.getSymbol())) {
                    return true;
                }
            }
        } catch (Exception e) {
            BRReportsManager.reportBug(new NullPointerException("isIsoErc20出错"), true);
            return false;

        }


        return false;
    }

    public boolean wipeKeyStore(Context context) {
        MyLog.d( "wipeKeyStore");
        return BRKeyStore.resetWalletKeyStore(context);
    }

    /**
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(ctx, 0);
                //if not authenticated, an error will be thrown and returned false, so no worry about mistakenly removing the wallet
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (UserNotAuthenticatedException e) {
                return false;
            }

        }
        return false;
    }

    public boolean noWalletForPlatform(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);
        return pubkey == null || pubkey.length == 0;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public boolean isNetworkAvailable(Context ctx) {
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    public void wipeWalletButKeystore(final Context ctx) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                List<BaseWalletManager> list = new ArrayList<>(getAllWallets(ctx));
                for (BaseWalletManager wallet : list) {
                    wallet.wipeData(ctx);
                }
            }
        });
    }

    public void wipeAll(Context app) {
        wipeKeyStore(app);
        wipeWalletButKeystore(app);
    }

    public void refreshBalances(Context app) {
        long start = System.currentTimeMillis();

        List<BaseWalletManager> list = new ArrayList<>(getAllWallets(app));
        for (BaseWalletManager wallet : list) {
            wallet.refreshCachedBalance(app);
        }
    }

    public void setSpendingLimitIfNotSet(final Context app, final BaseWalletManager wm) {
        if (app == null) return;
        BigDecimal limit = BRKeyStore.getTotalLimit(app, wm.getIso());
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
                    BigDecimal totalSpent = wallet == null ? BigDecimal.ZERO : wallet.getTotalSent(app);
                    BigDecimal totalLimit = totalSpent.add(BRKeyStore.getSpendLimit(app, wm.getIso()));
                    BRKeyStore.putTotalLimit(app, totalLimit, wm.getIso());
                }
            });

        }
    }

    @WorkerThread
    public void initLastWallet(Context app) {
        long start = System.currentTimeMillis();
        if (app == null) {
            app = BreadApp.getBreadContext();
            if (app == null) {
                MyLog.e( "initLastWallet: FAILED, app is null");
                return;
            }
        }
        BaseWalletManager wallet = getWalletByIso(app, BRSharedPrefs.getCurrentWalletIso(app));
        if (wallet == null) wallet = getWalletByIso(app, "BTC");
        if (wallet != null)
            wallet.connect(app);
    }

    @WorkerThread
    public void updateFixedPeer(Context app, BaseWalletManager wm) {
        String node = BRSharedPrefs.getTrustNode(app, wm.getIso());
        if (!Utils.isNullOrEmpty(node)) {
            String host = TrustedNode.getNodeHost(node);
            int port = TrustedNode.getNodePort(node);
//        MyLog.e( "trust onClick: host:" + host);
//        MyLog.e( "trust onClick: port:" + port);
            boolean success = wm.useFixedNode(host, port);
            if (!success) {
                MyLog.e( "updateFixedPeer: Failed to updateFixedPeer with input: " + node);
            } else {
                MyLog.d( "updateFixedPeer: succeeded");
            }
        }
        wm.connect(app);

    }

    /**
     *
     * 检测手有没有设置锁屏
     * @param app
     */
    public void startTheWalletIfExists(final Activity app) {
        final WalletsMaster m = WalletsMaster.getInstance(app);
        if (!m.isPasscodeEnabled(app)) {
//        if (false) {
            //Device passcode/password should be enabled for the app to work
            BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.Prompts_NoScreenLock_body_android),
                    app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            app.finish();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            app.finish();
                        }
                    }, 0);
        } else {
            if (!m.noWallet(app)) {
                BRAnimator.startBreadActivity(app, true);
            }
            //else just sit in the intro screen

        }
    }

}
