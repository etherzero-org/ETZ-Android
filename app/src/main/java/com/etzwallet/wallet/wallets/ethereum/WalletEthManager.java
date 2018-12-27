package com.etzwallet.wallet.wallets.ethereum;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import com.dapp.DappTransaction;
import com.etzwallet.BreadApp;
import com.etzwallet.BuildConfig;
import com.etzwallet.R;
import com.etzwallet.core.BRCoreMasterPubKey;
import com.etzwallet.core.ethereum.BREthereumAmount;
import com.etzwallet.core.ethereum.BREthereumLightNode;
import com.etzwallet.core.ethereum.BREthereumNetwork;
import com.etzwallet.core.ethereum.BREthereumToken;
import com.etzwallet.core.ethereum.BREthereumTransaction;
import com.etzwallet.core.ethereum.BREthereumBlock;
import com.etzwallet.core.ethereum.BREthereumWallet;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.LoadingDialog;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.CurrencyEntity;
import com.etzwallet.presenter.entities.TxUiHolder;
import com.etzwallet.presenter.interfaces.BROnSignalCompletion;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.exceptions.UserNotAuthenticatedException;
import com.etzwallet.tools.manager.BRApiManager;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.manager.InternetManager;
import com.etzwallet.tools.security.BRKeyStore;
import com.etzwallet.tools.security.PostAuth;
import com.etzwallet.tools.sqlite.RatesDataSource;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.tools.util.Bip39Reader;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.configs.WalletSettingsConfiguration;
import com.etzwallet.wallet.configs.WalletUiConfiguration;
import com.etzwallet.wallet.util.JsonRpcHelper;
import com.etzwallet.wallet.wallets.CryptoAddress;
import com.etzwallet.wallet.wallets.CryptoTransaction;

import com.etzwallet.wallet.wallets.WalletManagerHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip44.AddressIndex;
import io.github.novacrypto.bip44.BIP44;

import static com.etzwallet.tools.util.BRConstants.ROUNDING_MODE;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/21/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class WalletEthManager extends BaseEthereumWalletManager implements
        BREthereumLightNode.Client, BREthereumLightNode.Listener {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private CryptoTransaction mWatchedTransaction;
    private OnHashUpdated mWatchListener;

    private static final String ISO = "ETZ";
    public static final String ETH_SCHEME = "ethereum";
    //1ETH = 1000000000000000000 WEI
    public static final String ETHER_WEI = "1000000000000000000";
    //Max amount in ether
    public static final String MAX_ETH = "90000000";
    private final BigDecimal MAX_WEI = new BigDecimal(MAX_ETH).multiply(new BigDecimal(ETHER_WEI)); // 90m ETH * 18 (WEI)
    private final BigDecimal ONE_ETH = new BigDecimal(ETHER_WEI);
    private static final String NAME = "EtherZero";

    private static WalletEthManager mInstance;

    private WalletUiConfiguration mUiConfig;
    private WalletSettingsConfiguration mSettingsConfig;

    private BREthereumWallet mWallet;
    public BREthereumLightNode node;
    //    public String getAddress = "";
    public String pKey = null;

    private WalletEthManager(final Context app, byte[] ethPubKey, BREthereumNetwork network) {
        mUiConfig = new WalletUiConfiguration("#00BDFF", "#5E7AA3",
                true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);
        mSettingsConfig = new WalletSettingsConfiguration(app, ISO, getFingerprintLimits(app));

        if (Utils.isNullOrEmpty(ethPubKey)) {
            MyLog.e("WalletEthManager: Using the paperKey to create");
            String paperKey = null;
            try {
                paperKey = new String(BRKeyStore.getPhrase(app, 0));
                pKey = paperKey;


            } catch (UserNotAuthenticatedException e) {
                e.printStackTrace();
                return;
            }
            if (Utils.isNullOrEmpty(paperKey)) {
                alertDialog(app, "助记词为空！没有创建钱包！");
                MyLog.e("WalletEthManager: paper key is empty too, no wallet!");
                return;
            }
            String[] words = lookupWords(app, paperKey);

//            if (words.length<=0) {
//                alertDialog(app,"BIP39钱包地址是无效的!");
//                MyLog.e( "WalletEthManager: paper key does not validate with BIP39 Words for: "
//                        + Locale.getDefault().getLanguage());
//                return;
//            }

            String normalizedPhrase = Normalizer.normalize(paperKey.trim(), Normalizer.Form.NFKD);//paper 为NFKD格式


            node = new BREthereumLightNode(this, network, normalizedPhrase, words);
            node.addListener(this);

            mWallet = node.getWallet();

            if (null == mWallet) {
                alertDialog(app, "使用助记词创建ETZ钱包失败！");
                MyLog.e("WalletEthManager: failed to create the ETH wallet using paperKey.");
                return;
            }

            ethPubKey = mWallet.getAccount().getPrimaryAddressPublicKey();
            BRKeyStore.putEthPublicKey(ethPubKey, app);
        } else {
            MyLog.e("WalletEthManager: Using the pubkey to create");
            node = new BREthereumLightNode(this, network, ethPubKey);
            node.addListener(this);
            mWallet = node.getWallet();

            if (null == mWallet) {
                alertDialog(app, "使用保存的公钥创建ETZ钱包失败！");
                MyLog.e("WalletEthManager: failed to create the ETH wallet using saved publicKey.");
                return;
            }
        }

        mAddress = getReceiveAddress(app).stringify();
        MyLog.e("" + mAddress);

        Boolean isFirst = BRSharedPrefs.getFristCreate(app);
        MyLog.i("WalletEthManager: isFirst==" + isFirst);
        if (isFirst) {
            if (pKey == null || pKey.equals("")) {
                try {
                    pKey = new String(BRKeyStore.getPhrase(app, 0));
                } catch (UserNotAuthenticatedException e) {
                    e.printStackTrace();
                }
            }
            confirmAddress((Activity) app, pKey, mAddress);
        }
//        else{
//            BRSharedPrefs.putFirstCreate(app, false);
//        }


        if (Utils.isNullOrEmpty(mAddress)) {
            BRReportsManager.reportBug(new IllegalArgumentException("Eth address missing!"), true);
        }
        BreadApp.generateWalletIfIfNeeded(app, mAddress);

        WalletsMaster.getInstance(app).setSpendingLimitIfNotSet(app, this);

        estimateGasPrice();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);
        node.connect();

    }

    private void confirmAddress(final Activity app, String mn, String addr) {
        MyLog.i("WalletEthManager: addr==" + addr);
        AddressIndex addressIndex = BIP44
                .m()
                .purpose44()
                .coinType(60)
                .account(0)
                .external()
                .address(0);
        ExtendedPrivateKey rootKey = ExtendedPrivateKey.fromSeed(new SeedCalculator().calculateSeed(mn, ""), Bitcoin.MAIN_NET);
//        String extendedBase58 = rootKey.extendedBase58();
        ExtendedPrivateKey childPrivateKey = rootKey.derive(addressIndex, AddressIndex.DERIVATION);
//        String childExtendedBase58 = childPrivateKey.extendedBase58();
        byte[] privateKeyBytes = childPrivateKey.getKey();
        ECKeyPair keyPair = ECKeyPair.create(privateKeyBytes);
//        String privateKey = childPrivateKey.getPrivateKey();
//        String publicKey = childPrivateKey.neuter().getPublicKey();
        String address = Keys.getAddress(keyPair);
        String fullAddress = "0x" + address;


        if (!fullAddress.equals(addr.toLowerCase())) {
            alertDialog(app, app.getString(R.string.Alert_keystore_generic_android_bug));
            MyLog.i("WalletEthManager: mAddress=1000==地址不一致");
        }
        BRSharedPrefs.putFirstCreate(app, false);
        MyLog.i("WalletEthManager: mAddress=1000==" + addr.toLowerCase());
        MyLog.i("WalletEthManager: mAddress==2000=" + fullAddress);
    }

    private void alertDialog(Context app, String dialogContent) {
        final Activity app2 = (Activity) app;
        BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error),
                dialogContent,
                app.getString(R.string.Button_ok),
                null,
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        app2.finish();
                    }
                }, null, new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        app2.finish();
                    }
                }, 0);
        BRSharedPrefs.putAddressError(app, false);
    }

    public static synchronized WalletEthManager getInstance(Context app) {
        if (mInstance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                MyLog.e("getInstance: rawPubKey is null");
                return null;
            }
            byte[] ethPubKey = BRKeyStore.getEthPublicKey(app);
            if (Utils.isNullOrEmpty(ethPubKey)) {
                //check if there is a master key and if not means the wallet isn't created yet
                if (Utils.isNullOrEmpty(BRKeyStore.getMasterPublicKey(app))) {
                    return null;
                }
            }

            mInstance = new WalletEthManager(app, ethPubKey, BuildConfig.BITCOIN_TESTNET ? BREthereumNetwork.testnet : BREthereumNetwork.mainnet);

        }
        return mInstance;
    }

    public void estimateGasPrice() {
        mWallet.estimateGasPrice();
    }

    private String[] lookupWords(Context app, String paperKey) {
        //ignore l since it can be english but the phrase in other language.
        List<String> list = Bip39Reader.detectWords(app, paperKey);
        if (Utils.isNullOrEmpty(list) || (list.size() % Bip39Reader.WORD_LIST_SIZE != 0)) {
            String message = "lookupWords: " + "Failed: " + list + ", size: " + (list == null ? "null" : list.size());
            MyLog.e(message);
            BRReportsManager.reportBug(new IllegalArgumentException(message), true);
            return null;
        }
        String[] words = list.toArray(new String[list.size()]);


//        for (int i=0;i<words.length;i++){
//            MyLog.e("-----------------", words.length+"--words--"+words[i]);
//        }
//        MyLog.e("-----------------","paperKey="+ paperKey);
        if (BRCoreMasterPubKey.validateRecoveryPhrase(words, paperKey)) {
            // If the paperKey is valid for `words`, then return `words`
            return words;
        } else {
            // Otherwise, nothing
            BRReportsManager.reportBug(new NullPointerException("invalid paper key for words:" + paperKey.substring(0, 10))); //see a piece of it
            return null;
        }
    }

    private List<BigDecimal> getFingerprintLimits(Context app) {
        List<BigDecimal> result = new ArrayList<>();
        result.add(ONE_ETH.divide(new BigDecimal(100), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(ONE_ETH.divide(new BigDecimal(10), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(ONE_ETH);
        result.add(ONE_ETH.multiply(new BigDecimal(10)));
        result.add(ONE_ETH.multiply(new BigDecimal(100)));
        return result;
    }


    @Override
    public BREthereumAmount.Unit getUnit() {
        return BREthereumAmount.Unit.ETHER_WEI;
    }


    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] phrase) {
        mWallet.sign(tx.getEtherTx(), new String(phrase));
        mWallet.submit(tx.getEtherTx());
        String hash = tx.getEtherTx().getHash();
        MyLog.i("+++++++"+hash);
        return hash == null ? new byte[0] : hash.getBytes();
    }


    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
        mWatchedTransaction = tx;
        mWatchListener = listener;
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        return 3;
    }

    @Override
    public double getSyncProgress(long startHeight) {
        //Not needed for ETH, return fully synced always
        return 1.0;
    }

    @Override
    public double getConnectStatus() {
        //Not needed for ETH, return Connected always
        return 2;
    }

    @Override
    public void connect(Context app) {
        //Not needed for ETH
    }

    @Override
    public void disconnect(Context app) {
        //Not needed for ETH
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        //Not needed for ETH
        return false;
    }

    @Override
    public void rescan(Context app) {
        //Not needed for ETH
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        BREthereumTransaction[] txs = mWallet.getTransactions();
        CryptoTransaction[] arr = new CryptoTransaction[txs.length];
        for (int i = 0; i < txs.length; i++) {
            arr[i] = new CryptoTransaction(txs[i]);
        }

        return arr;
    }

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        return new BigDecimal(tx.getEtherTx().getFee(BREthereumAmount.Unit.ETHER_WEI));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) return null;
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            fee = BigDecimal.ZERO;
        } else {
            fee = new BigDecimal(mWallet.transactionEstimatedFee(amount.toPlainString()));
        }
        return fee;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return null;
    }

    @Override
    public String getTxAddress(CryptoTransaction tx) {
        return tx.getEtherTx().getTargetAddress();
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        BigDecimal balance = getCachedBalance(app);
        if (balance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        BigDecimal fee = new BigDecimal(mWallet.transactionEstimatedFee(balance.toPlainString()));
        if (fee.compareTo(balance) > 0) return BigDecimal.ZERO;
        return balance.subtract(fee);
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return new BigDecimal(1); //1 WEI
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        return new BigDecimal(tx.getEtherTx().getAmount());
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(1); //1 WEI
    }

    @Override
    public void updateFee(Context app) {

        if (app == null) {
            app = BreadApp.getBreadContext();

            if (app == null) {
                MyLog.d("updateFee: FAILED, app is null");
                return;
            }
        }

        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso());

        if (jsonString == null || jsonString.isEmpty()) {
            MyLog.e("updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }

        BigDecimal fee;
        BigDecimal economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = new BigDecimal(obj.getString("fee_per_kb"));
            economyFee = new BigDecimal(obj.getString("fee_per_kb_economy"));
            MyLog.d("updateFee: " + getIso() + ":" + fee + "|" + economyFee);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                BRSharedPrefs.putFeeRate(app, getIso(), fee);
                BRSharedPrefs.putFeeTime(app, getIso(), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                BRReportsManager.reportBug(new NullPointerException("Fee is weird:" + fee));
                MyLog.d("Error: Fee is unexpected value");

            }
            if (economyFee.compareTo(BigDecimal.ZERO) > 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(), economyFee);
            } else {
                BRReportsManager.reportBug(new NullPointerException("Economy fee is weird:" + economyFee));
                MyLog.d("Error: Economy fee is unexpected value");
            }
        } catch (JSONException e) {
            MyLog.e("updateFeePerKb: FAILED:json= " + jsonString + "---->Exception:" + e);
        }

    }

    @Override
    public void refreshCachedBalance(final Context app) {
        if (mWallet != null) {
            final BigDecimal balance = new BigDecimal(mWallet.getBalance(getUnit()));
            BRSharedPrefs.putCachedBalance(app, getIso(), balance);
        } else {
            final BigDecimal b = new BigDecimal('0');
            BRSharedPrefs.putCachedBalance(app, getIso(), b);
        }
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        BREthereumTransaction txs[] = mWallet.getTransactions();
        if (txs == null || txs.length <= 0) return null;
        List<TxUiHolder> uiTxs = new ArrayList<>();
        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
        long nonce=BRSharedPrefs.getAddressNonce(app,wm.getAddress());
        long lastNonce=-1;
        for (int i = txs.length - 1; i >= 0; i--) { //revere order
            BREthereumTransaction tx = txs[i];
        if(tx.getNonce()>=nonce&&!(tx.getTargetAddress().equalsIgnoreCase(mWallet.getAccount().getPrimaryAddress())))continue;
        if (lastNonce==tx.getNonce()&&!(tx.getTargetAddress().equalsIgnoreCase(mWallet.getAccount().getPrimaryAddress())))continue;
            uiTxs.add(new TxUiHolder(tx,
                    tx.getTargetAddress().equalsIgnoreCase(mWallet.getAccount().getPrimaryAddress()),
                    tx.getBlockTimestamp(),
                    (int) tx.getBlockNumber(),
                    Utils.isNullOrEmpty(tx.getHash()) ? null : tx.getHash().getBytes(),
                    tx.getHash(),
                    new BigDecimal(tx.getFee(BREthereumAmount.Unit.ETHER_WEI)),
                    tx.getTargetAddress(),
                    tx.getSourceAddress(),
                    null,
                    0,
                    new BigDecimal(tx.getAmount(BREthereumAmount.Unit.ETHER_WEI)),
                    true,
                    tx.getNonce()));
            lastNonce=tx.getNonce();
        }

        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        return mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(address);
    }

    @Override
    public boolean addressIsUsed(String address) {
        //Not needed for ETH
        return false;
    }

    @Override
    public boolean generateWallet(Context app) {
        //Not needed for ETH
        return false;
    }

    @Override
    public String getSymbol(Context app) {
//        return BRConstants.symbolEther;
        return ISO;
    }

    @Override
    public String getIso() {
        return "ETZ";
    }

    @Override
    public String getScheme() {
        return "etherzero";
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDenominator() {
        return "1000000000000000000";
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return new CryptoAddress(mWallet.getAccount().getPrimaryAddress(), null);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address, String data, String gasL, String gasP) {
        MyLog.d("tx_data_is-1=" + data);
        BREthereumTransaction tx;

        if ((!Utils.isNullOrEmpty(gasP)&&!Utils.isNullOrEmpty(gasL))){
            tx = mWallet.createTransactionGeneric(address, amount.toPlainString(), BREthereumAmount.Unit.ETHER_WEI,gasP,BREthereumAmount.Unit.ETHER_WEI,gasL,data);
        }else{
            tx = mWallet.createTransaction(address, amount.toPlainString(), BREthereumAmount.Unit.ETHER_WEI);
        }
        return new CryptoTransaction(tx);
    }

    @Override
    public String decorateAddress(String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return WalletManagerHelper.MAX_DECIMAL_PLACES;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso());
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return BigDecimal.ZERO;
    }

    @Override
    public void wipeData(Context app) {
        MyLog.e("wipeData: ");
    }

    @Override
    public void syncStarted() {
        //Not needed for ETH
    }

    @Override
    public void syncStopped(String error) {
        //Not needed for ETH
    }

    @Override
    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return MAX_WEI;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return mUiConfig;
    }

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return mSettingsConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        BigDecimal fiatData = getFiatForEth(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) return null;
        return fiatData; //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) return null;
        MyLog.i(getCachedBalance(app) + "");
        return getFiatForSmallestCrypto(app, getCachedBalance(app), null);
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent != null) {
            //passed in a custom CurrencyEntity
            //get crypto amount
            BigDecimal cryptoAmount = amount.divide(ONE_ETH, 8, BRConstants.ROUNDING_MODE);
            //multiply by fiat rate
            return cryptoAmount.multiply(new BigDecimal(ent.rate));
        }
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(ONE_ETH, 8, BRConstants.ROUNDING_MODE);

        BigDecimal fiatData = getFiatForEth(app, cryptoAmount, iso);
        if (fiatData == null) return null;
        return fiatData;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        return getEthForFiat(app, fiatAmount, iso);

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        return amount.divide(ONE_ETH, 8, ROUNDING_MODE);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        return amount.multiply(ONE_ETH);
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        BigDecimal ethAmount = getEthForFiat(app, amount, iso);
        if (ethAmount == null) return null;
        return ethAmount.multiply(ONE_ETH);
    }

    //pass in a eth amount and return the specified amount in fiat
    //ETH rates are in BTC (thus this math)
    private BigDecimal getFiatForEth(Context app, BigDecimal ethAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
        if (btcRate == null) {
            MyLog.e("getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            MyLog.e("getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return ethAmount.multiply(new BigDecimal(ethBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    //pass in a fiat amount and return the specified amount in ETH
    //ETH rates are in BTC (thus this math)
    private BigDecimal getEthForFiat(Context app, BigDecimal fiatAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
        if (btcRate == null) {
            MyLog.e("getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            MyLog.e("getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return fiatAmount.divide(new BigDecimal(ethBtcRate.rate).multiply(new BigDecimal(btcRate.rate)), 8, BRConstants.ROUNDING_MODE);
    }


    /**
     * The JSON RPC callbacks
     * Implement JSON RPC methods synchronously
     */

    @Override
    public void getBalance(final int wid, final String address, final int rid) {
        MyLog.i("getBalance: address===" + address + ";wid=" + wid + ";rid=" + rid);
        MyLog.i("getBalance: mWallet===" + mAddress);
        BREthereumWallet wallet = this.node.getWalletByIdentifier(wid);
        BREthereumToken token = wallet.getToken();
        if (null == token)
            getEtherBalance(wallet, wid, address, rid);
        else
            getTokenBalance(wallet, wid, token.getAddress(), address, rid);
    }

    protected void getEtherBalance(final BREthereumWallet wallet, final int wid, final String address, final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethRpcUrl = JsonRpcHelper.getEthereumRpcUrl();
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.JSONRPC, "2.0");
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_BALANCE);
                    params.put(address);
                    params.put(JsonRpcHelper.LATEST);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {

                                JSONObject responseObject = new JSONObject(jsonResult);
                                MyLog.i("getETZBalance" + responseObject.toString());

                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String balance = responseObject.getString(JsonRpcHelper.RESULT);
                                    node.announceBalance(wid, balance, rid);
                                }
                            } else {
                                MyLog.e("onRpcRequestCompleted: jsonResult is null");
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    protected void getTokenBalance(final BREthereumWallet wallet, final int wid,
                                   final String contractAddress,
                                   final String address,
                                   final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            MyLog.e("getTokenBalance: App in background!");
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                String ethRpcUrl = JsonRpcHelper.createTokenTransactionsUrl(address, contractAddress);

                MyLog.i("run: token balance url==" + ethRpcUrl);

                final JSONObject payload = new JSONObject();
                try {
                    payload.put(JsonRpcHelper.ID, String.valueOf(rid));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequestGet(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {
                                JSONObject responseObject = new JSONObject(jsonResult);

                                MyLog.i("onRpcRequestCompleted: responseObject=11==" + responseObject);


                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String balance = responseObject.getString(JsonRpcHelper.RESULT);
                                    node.announceBalance(wid, balance, rid);

                                } else {
                                    MyLog.e("onRpcRequestCompleted: Response does not contain the key 'result'.");
                                }
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

                    }
                });
            }
        });
    }


    @Override
    public void getGasPrice(final int wid, final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                MyLog.d("Making rpc request to -> " + ethUrl);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_GAS_PRICE);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {
                                JSONObject responseObject = new JSONObject(jsonResult);

                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String gasPrice = responseObject.getString(JsonRpcHelper.RESULT);
                                    MyLog.d("onRpcRequestCompleted: getGasPrice: " + gasPrice);
                                    node.announceGasPrice(wid, gasPrice, rid);
                                }
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

                    }
                });
            }
        });

    }

    //web3中的gaslimit
    @Override
    public void getGasEstimate(final int wid, final int tid, final String to, final String amount, final String data, final int rid) {
        MyLog.i("getGasEstimate: to==" + to);
        MyLog.i("getGasEstimate: amount==" + amount);
        MyLog.i("getGasEstimate: rid==" + rid);
        MyLog.i("getGasEstimate: data==" + data);
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                MyLog.d("Making rpc request to -> " + ethUrl);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                params.put(to);
                params.put(amount);
                params.put(data);

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_ESTIMATE_GAS);
                    payload.put("jsonrpc", "2.0");
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);
                            MyLog.i("gasLimit=== " + responseObject);
                            if (responseObject.has(JsonRpcHelper.RESULT)) {
                                String gasEstimate = responseObject.getString(JsonRpcHelper.RESULT);

                                node.announceGasEstimate(wid, tid, gasEstimate, rid);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    @Override
    public void submitTransaction(final int wid, final int tid, final String rawTransaction, final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        if (Utils.isEmulatorOrDebug(BreadApp.getBreadContext())) {
            MyLog.i("submitTransaction: wid:" + wid);
            MyLog.i("submitTransaction: tid:" + tid);
//            MyLog.i("submitTransaction: rawTransaction:" + rawTransaction);
            MyLog.i("submitTransaction: rid:" + rid);
        }
        MyLog.i("submitTransaction: rawTransaction:" + rawTransaction);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = JsonRpcHelper.getEthereumRpcUrl();
                MyLog.d("Making rpc request to -> " + eth_url);

                JSONObject payload = new JSONObject();
                JSONArray params = new JSONArray();
                try {
                    payload.put(JsonRpcHelper.JSONRPC, "2.0");
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_SEND_RAW_TRANSACTION);
                    params.put(rawTransaction);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                MyLog.i("payload"+payload.toString());
                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String txHash = null;
                        int errCode = 0;
                        String errMessage = "";
                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {

                                JSONObject responseObject = new JSONObject(jsonResult);

                                MyLog.i("返回结果: " + responseObject);
                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    txHash = responseObject.getString(JsonRpcHelper.RESULT);
                                    MyLog.d("onRpcRequestCompleted: " + txHash);
                                    node.announceSubmitTransaction(wid, tid, txHash, rid);
                                } else if (responseObject.has(JsonRpcHelper.ERROR)) {
                                    JSONObject errObj = responseObject.getJSONObject(JsonRpcHelper.ERROR);
                                    errCode = errObj.getInt(JsonRpcHelper.CODE);
                                    errMessage = errObj.getString(JsonRpcHelper.MESSAGE);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        final String finalTxHash = txHash;
                        final String finalErrMessage = errMessage;
                        final int finalErrCode = errCode;
                        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                final Context app = BreadApp.getBreadContext();
                                if (app != null && app instanceof Activity) {
                                    if (!Utils.isNullOrEmpty(finalTxHash)) {
                                        PostAuth.stampMetaData(app, finalTxHash.getBytes());
                                        BRAnimator.showBreadSignal((Activity) app, app.getString(R.string.Alerts_sendSuccess),
                                                app.getString(R.string.Alerts_sendSuccessSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                                    @Override
                                                    public void onComplete() {
                                                        BRAnimator.killAllFragments((Activity) app);
                                                        if (app instanceof DappTransaction){
                                                            BRSharedPrefs.putlastDappHash(app, finalTxHash);
                                                            ((DappTransaction) app).finish();
                                                        }
                                                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                mWallet.updateBalance();

                                                            }
                                                        });
                                                    }
                                                });
                                    } else {
                                        String message;
                                        if (finalErrMessage.equals("") && finalErrCode == 0) {
                                            message=String.format(Locale.getDefault(), "(%d) %s", finalErrCode,R.string.GasLimit_invalid+"/"+R.string.GasPrice_invalid);
                                        } else {
                                            message = String.format(Locale.getDefault(), "(%d) %s", finalErrCode, finalErrMessage);
                                        }
                                        BRDialog.showSimpleDialog(app, app.getString(R.string.WipeWallet_failedTitle), message);
                                    }
                                } else {
                                    MyLog.e("submitTransaction: app is null or not an activity");
                                }
                            }
                        });

                    }
                });

            }
        });
    }

    @Override
    public void getTransactions(final String address, final int id) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethRpcUrl = JsonRpcHelper.createEthereumTransactionsUrl(address);
                MyLog.i("ethRpcUrl==: " + ethRpcUrl);
                final JSONObject payload = new JSONObject();
                try {
                    payload.put(JsonRpcHelper.ID, String.valueOf(id));
                    payload.put(JsonRpcHelper.ACCOUNT, address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequestGet(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        MyLog.i("onRpcRequestCompleted: responseObject=222==" + jsonResult);
                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                // Convert response into JsonArray of transactions
                                JSONObject transactions = new JSONObject(jsonResult);

                                JSONArray transactionsArray = transactions.getJSONArray(JsonRpcHelper.RESULT);

                                String txHash = "";
                                String txTo = "";
                                String txFrom = "";
                                String txContract = "";
                                String txValue = "";
                                String txGas = "";
                                String txGasPrice = "";
                                String txNonce = "";
                                String txGasUsed = "";
                                String txBlockNumber = "";
                                String txBlockHash = "";
                                String txData = "";
                                String txBlockConfirmations = "";
                                String txBlockTransactionIndex = "";
                                String txBlockTimestamp = "";
                                String txIsError = "";

                                // Iterate through the list of transactions and call node.announceTransaction()
                                // to notify the core
                                for (int i = 0; i < transactionsArray.length(); i++) {
                                    JSONObject txObject = transactionsArray.getJSONObject(i);

                                    MyLog.d("TxObject contains -> " + txObject.toString());

                                    if (txObject.has(JsonRpcHelper.HASH)) {
                                        txHash = txObject.getString(JsonRpcHelper.HASH);
                                        // MyLog.d( "TxObject Hash -> " + txHash);

                                    }

                                    if (txObject.has(JsonRpcHelper.TO)) {
                                        txTo = txObject.getString(JsonRpcHelper.TO);
                                        // MyLog.d( "TxObject to -> " + txTo);

                                    }

                                    if (txObject.has(JsonRpcHelper.FROM)) {
                                        txFrom = txObject.getString(JsonRpcHelper.FROM);
                                        // MyLog.d( "TxObject from -> " + txFrom);

                                    }

                                    if (txObject.has(JsonRpcHelper.CONTRACT_ADDRESS)) {
                                        if (!txObject.getString(JsonRpcHelper.CONTRACT_ADDRESS).equals("0"))
                                            txContract = txObject.getString(JsonRpcHelper.CONTRACT_ADDRESS);
                                        // MyLog.d( "TxObject contractAddress -> " + txContract);

                                    }

                                    if (txObject.has(JsonRpcHelper.VALUE)) {
                                        txValue = txObject.getString(JsonRpcHelper.VALUE);
                                        // MyLog.d( "TxObject value -> " + txValue);

                                    }

                                    if (txObject.has(JsonRpcHelper.GAS)) {
                                        txGas = txObject.getString(JsonRpcHelper.GAS);
                                        // MyLog.d( "TxObject gas -> " + txGas);


                                    }

                                    if (txObject.has(JsonRpcHelper.GAS_PRICE)) {
                                        txGasPrice = txObject.getString(JsonRpcHelper.GAS_PRICE);
                                        // MyLog.d( "TxObject gasPrice -> " + txGasPrice);

                                    }

                                    if (txObject.has(JsonRpcHelper.NONCE)) {
                                        txNonce = txObject.getString(JsonRpcHelper.NONCE);
                                        // MyLog.d( "TxObject nonce -> " + txNonce);

                                    }

                                    if (txObject.has(JsonRpcHelper.GAS_USED)) {
                                        txGasUsed = txObject.getString(JsonRpcHelper.GAS_USED);
                                        // MyLog.d( "TxObject gasUsed -> " + txGasUsed);

                                    }

                                    if (txObject.has(JsonRpcHelper.BLOCK_NUMBER)) {
                                        txBlockNumber = txObject.getString(JsonRpcHelper.BLOCK_NUMBER);
                                        // MyLog.d( "TxObject blockNumber -> " + txBlockNumber);

                                    }

                                    if (txObject.has(JsonRpcHelper.BLOCK_HASH)) {
                                        txBlockHash = txObject.getString(JsonRpcHelper.BLOCK_HASH);
                                        // MyLog.d( "TxObject blockHash -> " + txBlockHash);

                                    }

                                    if (txObject.has(JsonRpcHelper.INPUT)) {
                                        txData = txObject.getString(JsonRpcHelper.INPUT);
                                        // MyLog.d( "TxObject input -> " + txData);

                                    }

                                    if (txObject.has(JsonRpcHelper.CONFIRMATIONS)) {
                                        txBlockConfirmations = txObject.getString(JsonRpcHelper.CONFIRMATIONS);
                                        // MyLog.d( "TxObject confirmations -> " + txBlockConfirmations);

                                    }

                                    if (txObject.has(JsonRpcHelper.TRANSACTION_INDEX)) {
                                        txBlockTransactionIndex = txObject.getString(JsonRpcHelper.TRANSACTION_INDEX);
                                        // MyLog.d( "TxObject transactionIndex -> " + txBlockTransactionIndex);

                                    }

                                    if (txObject.has(JsonRpcHelper.TIMESTAMP)) {
                                        txBlockTimestamp = txObject.getString(JsonRpcHelper.TIMESTAMP);
                                        // MyLog.d( "TxObject blockTimestamp -> " + txBlockTimestamp);

                                    }

                                    if (txObject.has(JsonRpcHelper.IS_ERROR)) {
                                        txIsError = txObject.getString(JsonRpcHelper.IS_ERROR);
                                        // MyLog.d( "TxObject isError -> " + txIsError);

                                    }

                                    node.announceTransaction(id, txHash,
                                            (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txFrom) ? address : txFrom),
                                            (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txTo) ? address : txTo),
                                            txContract, txValue, txGas, txGasPrice, txData, txNonce, txGasUsed, txBlockNumber, txBlockHash, txBlockConfirmations, txBlockTransactionIndex, txBlockTimestamp, txIsError);
                                    Context app = BreadApp.getBreadContext();

                                    int blockHeight = (int) node.getBlockHeight();
                                    if (app != null && blockHeight != Integer.MAX_VALUE && blockHeight > 0) {
                                        MyLog.i("onRpcRequestCompleted: responseObject=222==" + blockHeight);
                                        BRSharedPrefs.putLastBlockHeight(app, getIso(), blockHeight);
                                    }
                                }

                                MyLog.d("Rpc Transactions array length -> " + transactionsArray.length());
                            } catch (JSONException e) {
                                MyLog.e("Exception: " + e);

                            }
                        }
                    }
                });
            }
        });

    }

    @Override
    public void getLogs(final String contract, final String address, final String event, final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        String contractAddress="";
        if (!Utils.isNullOrEmpty(contract)) {
            contractAddress = contract;
        }else {
            Activity ctx=(Activity) BreadApp.getBreadContext();
            String iso=BRSharedPrefs.getCurrentWalletIso(ctx);
            MyLog.i("run: ethRpcUrl=ethRpcUrl===" + iso);
            WalletsMaster wallet=WalletsMaster.getInstance(ctx);
            if( wallet.isIsoErc20(ctx,iso)){
                try {
                    BREthereumToken[] tokens=WalletEthManager.getInstance(ctx).node.tokens;
                    for (BREthereumToken t : tokens) {
                        if (t.getSymbol().equalsIgnoreCase(iso)){
                            contractAddress=t.getAddress();
                        }
                    }
                }catch (Exception e){
                    MyLog.i("run: ethRpcUrl=ethRpcUrl==="+e);
                }


            }
        }

        final String finalContractAddress = contractAddress;
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethRpcUrl = JsonRpcHelper.createLogsUrl(address, finalContractAddress, event);
                MyLog.i("run: ethRpcUrl=ethRpcUrl===" + ethRpcUrl);
                MyLog.i("run: ethRpcUrl=event===" + event);
                MyLog.i("run: ethRpcUrl=contract===" + finalContractAddress);
                MyLog.i("run: ethRpcUrl=address===" + address);

                final JSONObject payload = new JSONObject();
                try {
                    payload.put(JsonRpcHelper.ID, String.valueOf(rid));
                    // ?? payload.put("account", address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequestGet(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        MyLog.i("run: ethRpcUrl=logs===" + jsonResult);
                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                // Convert response into JsonArray of logs
                                JSONObject logs = new JSONObject(jsonResult);
                                MyLog.i("run: ethRpcUrl=logs===" + logs);
                                JSONArray logsArray = logs.getJSONArray(JsonRpcHelper.RESULT);

                                // Iterate through the list of transactions and call node.announceTransaction()
                                // to notify the core
                                for (int i = 0; i < logsArray.length(); i++) {
                                    JSONObject log = logsArray.getJSONObject(i);

                                    MyLog.d("LogObject contains -> " + log.toString());

                                    JSONArray topicsArray = log.getJSONArray(JsonRpcHelper.TOPICS);
                                    String[] topics = new String[topicsArray.length()];
                                    for (int dex = 0; dex < topics.length; dex++) {
                                        topics[dex] = topicsArray.getString(dex);
                                    }

                                    node.announceLog(rid,
                                            log.getString(JsonRpcHelper.TRANSACTION_HASH),
                                            log.getString(JsonRpcHelper.ADDRESS), // contract
                                            topics,
                                            log.getString(JsonRpcHelper.DATA),
                                            log.getString(JsonRpcHelper.GAS_PRICE),
                                            log.getString(JsonRpcHelper.GAS_USED),
                                            log.getString(JsonRpcHelper.LOG_INDEX),
                                            log.getString(JsonRpcHelper.BLOCK_NUMBER),
                                            log.getString(JsonRpcHelper.TRANSACTION_INDEX),
                                            log.getString(JsonRpcHelper.TIMESTAMP));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void getBlockNumber(final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = JsonRpcHelper.getEthereumRpcUrl();
                MyLog.d("Making rpc request to -> " + eth_url);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_BLOCK_NUMBER);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has(JsonRpcHelper.RESULT)) {
                                String blockNumber = responseObject.getString(JsonRpcHelper.RESULT);
                                MyLog.e("onRpcRequestCompleted: getBlockNumber: " + blockNumber);
                                node.announceBlockNumber(blockNumber, rid);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public BREthereumLightNode getNode() {
        return node;
    }

    private void printInfo(String infoText, String walletIso, String eventName) {
        MyLog.d(String.format("%s (%s): %s", eventName, walletIso, infoText));
    }

    @Override
    public void handleWalletEvent(BREthereumWallet wallet, WalletEvent event,
                                  Status status,
                                  String errorDescription) {
        Context app = BreadApp.getBreadContext();

        if (app != null) {
            String iso = (null == wallet.getToken() ? getIso() : wallet.getToken().getSymbol());
            switch (event) {
                case CREATED:
                    printInfo("Wallet Created", iso, event.name());
                    break;
                case BALANCE_UPDATED:
                    if (status == Status.SUCCESS) {
                        notifyBalanceWasUpdated(wallet, iso);
                        printInfo("New Balance: " + wallet.getBalance(), iso, event.name());
                    } else {
                        BRReportsManager.reportBug(new IllegalArgumentException("BALANCE_UPDATED: Failed to update balance: status:"
                                + status + ", err: " + errorDescription));
                    }
                    break;
                case DEFAULT_GAS_LIMIT_UPDATED:
                    printInfo("New Gas Limit: ...", iso, event.name());
                    break;
                case DEFAULT_GAS_PRICE_UPDATED:
                    printInfo("New Gas Price: " + BRSharedPrefs.getFeeRate(app, getIso()), iso, event.name());
                    break;
                case DELETED:
                    BRReportsManager.reportBug(new NullPointerException("Wallet was deleted:" + event.name()));
                    printInfo("Deleted: ", iso, event.name());
                    break;
            }
        }
    }

    @Override
    public void handleBlockEvent(BREthereumBlock block, BlockEvent event,
                                 Status status,
                                 String errorDescription) {
        MyLog.d("handleBlockEvent: " + block + ", event: " + event);
        Context app = BreadApp.getBreadContext();
        if (app != null) {
            //String iso = (null == wallet.getToken() ? "ETH" : wallet.getToken().getSymbol());

            switch (event) {
                case CREATED:
                    printInfo("Block created: " + block.getNumber(), "UNK", event.name());
                    break;
                case DELETED:
                    printInfo("Block deleted: " + block.getNumber(), "UNK", event.name());
                    break;
            }
        }
    }

    @Override
    public void handleTransactionEvent(BREthereumWallet wallet,
                                       BREthereumTransaction transaction,
                                       TransactionEvent event,
                                       Status status,
                                       String errorDescription) {
        Context app = BreadApp.getBreadContext();

        if (app != null) {
            String iso = (null == wallet.getToken() ? getIso() : wallet.getToken().getSymbol());
            switch (event) {
                case ADDED:
                    printInfo("New transaction added: ", iso, event.name());
                    getWalletManagerHelper().onTxListModified(transaction.getHash());
                    break;
                case REMOVED:
                    printInfo("Transaction removed: ", iso, event.name());
                    getWalletManagerHelper().onTxListModified(transaction.getHash());
                    break;
                case CREATED:
                    printInfo("Transaction created: " + transaction.getAmount(), iso, event.name());
                    break;
                case SIGNED:
                    printInfo("Transaction signed: " + transaction.getAmount(), iso, event.name());
                    break;
                case SUBMITTED:
                    if (mWatchedTransaction != null) {
                        MyLog.e("handleTransactionEvent: mWatchedTransaction: " + mWatchedTransaction.getEtherTx().getNonce()
                                + ", actual: " + transaction.getNonce());
                        if (mWatchedTransaction.getEtherTx().getNonce() == transaction.getNonce()) {
                            String hash = transaction.getHash();
                            if (!Utils.isNullOrEmpty(hash)) {
                                if (mWatchListener != null)
                                    mWatchListener.onUpdated(hash);
                                mWatchListener = null;
                                mWatchedTransaction = null;
                            }
                        }
                    } else {
                        MyLog.e("handleTransactionEvent: tx is null");
                    }
                    MyLog.e("handleTransactionEvent: SUBMITTED: " + transaction.getHash());
                    printInfo("Transaction submitted: " + transaction.getAmount(), iso, event.name());
                    break;
                case BLOCKED:
                    printInfo("Transaction blocked: " + transaction.getAmount(), iso, event.name());
                    break;
                case ERRORED:
                    printInfo("Transaction error: " + transaction.getAmount(), iso, event.name());
                    break;
                case GAS_ESTIMATE_UPDATED:
                    printInfo("Transaction gas estimate updated: " + transaction.getAmount(), iso, event.name());
                    break;
                case BLOCK_CONFIRMATIONS_UPDATED:
                    printInfo("Transaction confirmations updated: " + transaction.getBlockConfirmations(), iso, event.name());
                    break;
            }
        }
    }

    @Override
    public void getNonce(final String address, final int rid) {
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                MyLog.d("Making rpc request to -> " + ethUrl);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_TRANSACTION_COUNT);
                    params.put(address);
                    params.put(JsonRpcHelper.LATEST);  // or "pending" ?
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has(JsonRpcHelper.RESULT)) {
                                String nonce = responseObject.getString(JsonRpcHelper.RESULT);

                                if (nonce.length()>=3) {
                                    MyLog.d("onRpcRequestCompleted: getNonce: " + Integer.parseInt(nonce.substring(2, nonce.length()), 16));
                                    BRSharedPrefs.setAddressNonce(BreadApp.getBreadContext(), address, Integer.parseInt(nonce.substring(2, nonce.length()), 16));
                                }
                                node.announceNonce(address, nonce, rid);

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }


    /**
     * refresh current wallet's balance by code
     *
     * @param code - wallet code for which the balance was updated
     */
    private void notifyBalanceWasUpdated(BREthereumWallet wallet, String code) {
        if (Utils.isNullOrEmpty(code)) {
            BRReportsManager.reportBug(new NullPointerException("Invalid code: " + code));
            return;
        }
        final Context context = BreadApp.getBreadContext();
        if (getIso().equalsIgnoreCase(code)) {
            //ETH wallet balance was updated

            final BigDecimal balance = new BigDecimal(wallet.getBalance(getUnit()));
            setCachedBalance(context, balance);
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    onBalanceChanged(balance); //this, Eth wallet.
                }
            });

        } else {
            //ERC20 wallet balance was updated

            final BigDecimal balance = new BigDecimal(wallet.getBalance(BREthereumAmount.Unit.TOKEN_DECIMAL)); //use TOKEN_DECIMAL
            final String iso = wallet.getToken().getSymbol();
            if (context != null) {
                final BaseWalletManager wm = WalletsMaster.getInstance(context).getWalletByIso(context, iso);
                if (wm != null) {
                    wm.setCachedBalance(context, balance);
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            wm.onBalanceChanged(balance);
                        }
                    });
                } else {
                    BRReportsManager.reportBug(new NullPointerException("Could not find wallet with code: " + code));
                }

            }
        }
    }

}
