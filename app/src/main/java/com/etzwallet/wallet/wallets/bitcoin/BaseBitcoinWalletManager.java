package com.etzwallet.wallet.wallets.bitcoin;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.core.BRCoreAddress;
import com.etzwallet.core.BRCoreChainParams;
import com.etzwallet.core.BRCoreKey;
import com.etzwallet.core.BRCoreMasterPubKey;
import com.etzwallet.core.BRCoreMerkleBlock;
import com.etzwallet.core.BRCorePeer;
import com.etzwallet.core.BRCorePeerManager;
import com.etzwallet.core.BRCoreTransaction;
import com.etzwallet.core.BRCoreWallet;
import com.etzwallet.core.BRCoreWalletManager;
import com.etzwallet.core.ethereum.BREthereumAmount;
import com.etzwallet.presenter.customviews.BRToast;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.BRMerkleBlockEntity;
import com.etzwallet.presenter.entities.BRPeerEntity;
import com.etzwallet.presenter.entities.BRTransactionEntity;
import com.etzwallet.presenter.entities.BlockEntity;
import com.etzwallet.presenter.entities.CurrencyEntity;
import com.etzwallet.presenter.entities.PeerEntity;
import com.etzwallet.presenter.entities.TxUiHolder;
import com.etzwallet.presenter.interfaces.BROnSignalCompletion;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.manager.BRApiManager;
import com.etzwallet.tools.manager.BRNotificationManager;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.manager.InternetManager;
import com.etzwallet.tools.sqlite.BtcBchTransactionDataStore;
import com.etzwallet.tools.sqlite.MerkleBlockDataSource;
import com.etzwallet.tools.sqlite.PeerDataSource;
import com.etzwallet.tools.sqlite.RatesDataSource;
import com.etzwallet.tools.sqlite.TransactionStorageManager;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.tools.util.CurrencyUtils;
import com.etzwallet.tools.util.TypesConverter;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.abstracts.OnBalanceChangedListener;
import com.etzwallet.wallet.abstracts.OnTxListModified;
import com.etzwallet.wallet.abstracts.SyncListener;
import com.etzwallet.wallet.configs.WalletSettingsConfiguration;
import com.etzwallet.wallet.configs.WalletUiConfiguration;
import com.etzwallet.wallet.wallets.CryptoAddress;
import com.etzwallet.wallet.wallets.CryptoTransaction;
import com.etzwallet.wallet.wallets.WalletManagerHelper;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.etzwallet.tools.util.BRConstants.ROUNDING_MODE;

public abstract class BaseBitcoinWalletManager extends BRCoreWalletManager implements BaseWalletManager {

    private static final String TAG = BaseBitcoinWalletManager.class.getSimpleName();

    public static final int ONE_BITCOIN_IN_SATOSHIS = 100000000; // 1 Bitcoin in satoshis, 100 millions
    private static final long MAXIMUM_AMOUNT = 21000000; // Maximum number of coins available
    private static final int SYNC_MAX_RETRY = 3;

    public static final String BITCOIN_SYMBOL = "BTC";
    public static final String BITCASH_SYMBOL = "BCH";

    private WalletManagerHelper mWalletManagerHelper;
    private int mSyncRetryCount = 0;
    private static final int CREATE_WALLET_MAX_RETRY = 3;
    private int mCreateWalletAllowedRetries = CREATE_WALLET_MAX_RETRY;
    private WalletUiConfiguration mUiConfig;
    private WalletSettingsConfiguration mSettingsConfig;
    private Executor mListenerExecutor = Executors.newSingleThreadExecutor();

    public enum RescanMode {
        FROM_BLOCK, FROM_CHECKPOINT, FULL
    }

    BaseBitcoinWalletManager(Context context, BRCoreMasterPubKey masterPubKey, BRCoreChainParams chainParams, double earliestPeerTime) {
        super(masterPubKey, chainParams, earliestPeerTime);
        mWalletManagerHelper = new WalletManagerHelper();

        MyLog.d( "connectWallet:" + Thread.currentThread().getName());
        if (context == null) {
            MyLog.e( "connectWallet: context is null");
            return;
        }
        String firstAddress = masterPubKey.getPubKeyAsCoreKey().address();
        BRSharedPrefs.putFirstAddress(context, firstAddress);

        mUiConfig = new WalletUiConfiguration(getColor(), null, true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);
        mSettingsConfig = new WalletSettingsConfiguration(context, getIso(), getFingerprintLimits(context));
    }

    protected abstract String getTag();

    protected abstract String getColor();

    protected abstract List<BigDecimal> getFingerprintLimits(Context context);

    protected BRCoreWallet createWalletRetry() {
        Context app = BreadApp.getBreadContext();
        if (0 == mCreateWalletAllowedRetries) {
            // The app is dead - tell the user...
            BRDialog.showSimpleDialog(app, "Wallet error!", "please contact support@breadwallet.com");
            // ... for now just this.  App crashes after this
            return null;
        }

        mCreateWalletAllowedRetries--;

        // clear out the SQL data - ensure that loadTransaction returns an empty array
        // mark this Manager a needing a sync.
        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, getIso());
        BRReportsManager.reportBug(new RuntimeException("Wallet creation failed, after clearing tx size: " + loadTransactions().length));
        // Try again
        return createWallet();
    }

    @Override
    protected BRCoreWallet.Listener createWalletListener() {
        return new BRCoreWalletManager.WrappedExecutorWalletListener(
                super.createWalletListener(),
                mListenerExecutor);
    }

    @Override
    protected BRCorePeerManager.Listener createPeerManagerListener() {
        return new BRCoreWalletManager.WrappedExecutorPeerManagerListener(
                super.createPeerManagerListener(),
                mListenerExecutor);
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        BRCoreTransaction[] txs = getWallet().getTransactions();
        CryptoTransaction[] arr = new CryptoTransaction[txs.length];
        for (int i = 0; i < txs.length; i++) {
            arr[i] = new CryptoTransaction(txs[i]);
        }

        return arr;
    }

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        return new BigDecimal(getWallet().getTransactionFee(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) {
            return null;
        }
        if (amount.longValue() == 0) {
            fee = BigDecimal.ZERO;
        } else {
            CryptoTransaction tx = null;
            if (isAddressValid(address)) {
                final  String d = "bitdata";
                tx = createTransaction(amount, address, d,null,null);
            }

            if (tx == null) {
                fee = new BigDecimal(getWallet().getFeeForTransactionAmount(amount.longValue()));
            } else {
                fee = getTxFee(tx);
                if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
                    fee = new BigDecimal(getWallet().getFeeForTransactionAmount(amount.longValue()));
                }
            }
        }
        return fee;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return new BigDecimal(getWallet().getFeeForTransactionSize(size.longValue()));
    }

    @Override
    public String getTxAddress(CryptoTransaction tx) {
        return getWallet().getTransactionAddress(tx.getCoreTx()).stringify();
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        return new BigDecimal(getWallet().getMaxOutputAmount());
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return new BigDecimal(getWallet().getMinOutputAmount());
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        return new BigDecimal(getWallet().getTransactionAmount(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(BRCoreTransaction.getMinOutputAmount());
    }

    @Override
    public void updateFee(Context app) {
        if (app == null) {
            app = BreadApp.getBreadContext();
            if (app == null) {
                MyLog.e( "updateFee: FAILED, app is null");
                return;
            }
        }
        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso());
        if (jsonString == null || jsonString.isEmpty()) {
            MyLog.e( "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }
        BigDecimal fee;
        BigDecimal economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = new BigDecimal(obj.getString("fee_per_kb"));
            economyFee = new BigDecimal(obj.getString("fee_per_kb_economy"));
            MyLog.d( "updateFee: " + getIso() + ":" + fee + "|" + economyFee);

            if (fee.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(new BigDecimal(getWallet().getMaxFeePerKb())) < 0) {
                BRSharedPrefs.putFeeRate(app, getIso(), fee);
                boolean favorStandardFee = BRSharedPrefs.getFavorStandardFee(app, getIso());
                getWallet().setFeePerKb(favorStandardFee ? fee.longValue() : economyFee.longValue());
                BRSharedPrefs.putFeeTime(app, getIso(), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                BRReportsManager.reportBug(new NullPointerException("Fee is weird:" + fee));
            }
            if (economyFee.compareTo(BigDecimal.ZERO) > 0 && economyFee.compareTo(new BigDecimal(getWallet().getMaxFeePerKb())) < 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(), economyFee);
            } else {
                BRReportsManager.reportBug(new NullPointerException("Economy fee is weird:" + economyFee));
            }
        } catch (JSONException e) {
            MyLog.e( "updateFeePerKb: FAILED: " + jsonString+ e);
            BRReportsManager.reportBug(e);
            BRReportsManager.reportBug(new IllegalArgumentException("JSON ERR: " + jsonString));
        }
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        BRCoreTransaction[] txs = getWallet().getTransactions();
        if (txs == null || txs.length <= 0) {
            return null;
        }
        List<TxUiHolder> uiTxs = new ArrayList<>();
        for (int i = txs.length - 1; i >= 0; i--) { //revere order
            BRCoreTransaction tx = txs[i];
            String toAddress = null;
            //if sent
            if (getWallet().getTransactionAmount(tx) < 0) {
                toAddress = tx.getOutputAddresses()[0];
            } else {
                for (String to : tx.getOutputAddresses()) {
                    if (containsAddress(to)) {
                        toAddress = to;
                        break;
                    }
                }
            }
            if (toAddress == null) {
                throw new NullPointerException("Failed to retrieve toAddress");
            }
            boolean isReceived = getWallet().getTransactionAmount(tx) > 0;
            if (!isReceived) {
                //store the latest send transaction blockheight
                BRSharedPrefs.putLastSendTransactionBlockheight(app, getIso(), tx.getBlockHeight());
            }
            uiTxs.add(new TxUiHolder(tx, isReceived, tx.getTimestamp(), (int) tx.getBlockHeight(), tx.getHash(),
                    tx.getReverseHash(), new BigDecimal(getWallet().getTransactionFee(tx)),
                    toAddress, tx.getInputAddresses()[0],
                    new BigDecimal(getWallet().getBalanceAfterTransaction(tx)), (int) tx.getSize(),
                    new BigDecimal(getWallet().getTransactionAmount(tx)), getWallet().transactionIsValid(tx),0));
        }

        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        return !Utils.isNullOrEmpty(address) && getWallet().containsAddress(new BRCoreAddress(address));
    }

    @Override
    public boolean addressIsUsed(String address) {
        return !Utils.isNullOrEmpty(address) && getWallet().addressIsUsed(new BRCoreAddress(address));
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.BITS_SYMBOL;
        if (app != null) {
            int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = "μ" + getIso();
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + getIso();
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = getIso();
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public abstract String getIso();

    @Override
    public abstract String getScheme();

    @Override
    public abstract String getName();

    @Override
    public String getDenominator() {
        return String.valueOf(ONE_BITCOIN_IN_SATOSHIS);
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        BRCoreAddress addr = getWallet().getReceiveAddress();
        return new CryptoAddress(addr.stringify(), addr);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address, String data, String gasL, String gasP) {
        if (Utils.isNullOrEmpty(address)) {
            MyLog.e( "createTransaction: can't create, address is null");
            return null;
        }
        BRCoreTransaction tx = getWallet().createTransaction(amount.longValue(), new BRCoreAddress(address),data, gasL, gasP);
        return tx == null ? null : new CryptoTransaction(tx);
    }

    @Override
    public abstract String decorateAddress(String address);

    @Override
    public abstract String undecorateAddress(String address);

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return 2;
            default:
                return WalletManagerHelper.MAX_DECIMAL_PLACES;
        }
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso());
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return new BigDecimal(getWallet().getTotalSent());
    }

    @Override
    public void wipeData(Context app) {
        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, getIso());
        MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, getIso());
        PeerDataSource.getInstance(app).deleteAllPeers(app, getIso());
        BRSharedPrefs.clearAllPrefs(app);
    }

    @Override
    public void setCachedBalance(final Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(), balance);
    }

    @Override
    public void refreshAddress(Context app) {
        BRCoreAddress address = getWallet().getReceiveAddress();
        if (Utils.isNullOrEmpty(address.stringify())) {
            MyLog.e( "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso());

    }

    @Override
    public void refreshCachedBalance(Context app) {
        BigDecimal balance = new BigDecimal(getWallet().getBalance());
        BRSharedPrefs.putCachedBalance(app, getIso(), balance);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return new BigDecimal(MAXIMUM_AMOUNT);
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
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), BRSharedPrefs.getPreferredFiatIso(app));
        if (ent == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(ent.rate); //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) {
            return null;
        }
        BigDecimal balance = getFiatForSmallestCrypto(app, getCachedBalance(app), null);
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(balance.doubleValue());
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount.doubleValue() == 0) {
            return amount;
        }
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent == null) {
            ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), iso);
        }
        if (ent == null) {
            return null;
        }
        double rate = ent.rate;
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount.doubleValue() == 0) {
            return fiatAmount;
        }
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), iso);
        if (ent == null) {
            return null;
        }
        double rate = ent.rate;
        //convert c to $.
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
        BigDecimal result = BigDecimal.ZERO;
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = fiatAmount.divide(new BigDecimal(rate), 2, ROUNDING_MODE).multiply(new BigDecimal("1000000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = fiatAmount.divide(new BigDecimal(rate), getMaxDecimalPlaces(app), ROUNDING_MODE);
                break;
        }
        return result;

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        BigDecimal result = BigDecimal.ZERO;
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.divide(new BigDecimal("100"), 2, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.divide(new BigDecimal("100000"), 5, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.divide(new BigDecimal("100000000"), 8, ROUNDING_MODE);
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        BigDecimal result = BigDecimal.ZERO;
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso());
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.multiply(new BigDecimal("100"));
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.multiply(new BigDecimal("100000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.multiply(new BigDecimal("100000000"));
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), iso);
        if (ent == null) {
            MyLog.e( "getSmallestCryptoForFiat: no exchange rate data!");
            return amount;
        }
        double rate = ent.rate;
        //convert c to $.
        return amount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }

    // TODO only ETH and ERC20
    @Override
    public BREthereumAmount.Unit getUnit() {
        throw new RuntimeException("stub");
    }
    @Override
    public String getAddress() {
        return BRSharedPrefs.getReceiveAddress(BreadApp.getBreadContext(), getIso());
    }
    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && new BRCoreAddress(address).isValid();
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed) {
        return super.signAndPublishTransaction(tx.getCoreTx(), seed);
    }

    // TODO only ETH and ERC20
    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        if (Utils.isNullOrEmpty(txHash)) return 0;
        return getPeerManager().getRelayCount(txHash);
    }

    @Override
    public double getSyncProgress(long startHeight) {
        return getPeerManager().getSyncProgress(startHeight);
    }

    @Override
    public double getConnectStatus() {
        BRCorePeer.ConnectStatus status = getPeerManager().getConnectStatus();
        if (status == BRCorePeer.ConnectStatus.Disconnected)
            return 0;
        else if (status == BRCorePeer.ConnectStatus.Connecting)
            return 1;
        else if (status == BRCorePeer.ConnectStatus.Connected)
            return 2;
        else if (status == BRCorePeer.ConnectStatus.Unknown)
            return 3;
        else
            throw new IllegalArgumentException();
    }

    @Override
    public void connect(Context app) {
        getPeerManager().connect();
    }

    @Override
    public void disconnect(Context app) {
        getPeerManager().disconnect();
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        return false;
    }

    /**
     * The rescan now operates in 3 modes (Stage 1: Incremental rescan):
     * <p>
     * 1)The latest block in which a send transaction originating from the user's wallet was confirmed.
     * If there are no sends, only the following two starting points will be used.
     * 2)The latest block checkpoint that is hardcoded in the app.
     * 3)500 blocks before the the block height at which the wallet was created (from KV store),
     * or the first block after the introduction of BIP39 if there is no date stored in the KV store.
     *
     * @param app
     */
    @Override
    public void rescan(Context app) {
        //the last time the app has done a rescan (not a regular scan)
        long lastRescanTime = BRSharedPrefs.getLastRescanTime(app, getIso());
        long now = System.currentTimeMillis();
        //the last rescan mode that was used for rescan
        String lastRescanModeUsedValue = BRSharedPrefs.getLastRescanModeUsed(app, getIso());
        //the last successful send transaction's blockheight (if there is one, 0 otherwise)
        long lastSentTransactionBlockheight = BRSharedPrefs.getLastSendTransactionBlockheight(app, getIso());
        //was the rescan used within the last 24 hours
        boolean wasLastRescanWithin24h = now - lastRescanTime <= DateUtils.DAY_IN_MILLIS;

        if (wasLastRescanWithin24h) {
            if (isModeSame(RescanMode.FROM_BLOCK, lastRescanModeUsedValue)) {
                rescan(app, RescanMode.FROM_CHECKPOINT);
            } else if (isModeSame(RescanMode.FROM_CHECKPOINT, lastRescanModeUsedValue)) {
                rescan(app, RescanMode.FULL);
            }
        } else {
            if (lastSentTransactionBlockheight > 0) {
                rescan(app, RescanMode.FROM_BLOCK);
            } else {
                rescan(app, RescanMode.FROM_CHECKPOINT);
            }
        }
    }

    /**
     * Trigger the appropriate rescan and save the name and time to BRSharedPrefs
     *
     * @param app  android context to use
     * @param mode the RescanMode to be used
     */
    private void rescan(Context app, RescanMode mode) {
        if (RescanMode.FROM_BLOCK == mode) {
            long lastSentTransactionBlockheight = BRSharedPrefs.getLastSendTransactionBlockheight(app, getIso());
            MyLog.d( "rescan -> with last block: " + lastSentTransactionBlockheight);
            getPeerManager().rescanFromBlock(lastSentTransactionBlockheight);
        } else if (RescanMode.FROM_CHECKPOINT == mode) {
            MyLog.e( "rescan -> from checkpoint");
            getPeerManager().rescanFromCheckPoint();
        } else if (RescanMode.FULL == mode) {
            MyLog.e( "rescan -> full");
            getPeerManager().rescan();
        } else {
            throw new IllegalArgumentException("RescanMode is invalid, mode -> " + mode);
        }
        long now = System.currentTimeMillis();
        BRSharedPrefs.putLastRescanModeUsed(app, getIso(), mode.name());
        BRSharedPrefs.putLastRescanTime(app, getIso(), now);
    }

    /**
     * @param mode       the RescanMode enum to compare to
     * @param stringMode the stored enum value
     * @return true if the same mode
     */
    private boolean isModeSame(RescanMode mode, String stringMode) {
        if (stringMode == null) {
            //prevent NPE
            stringMode = "";
        }
        try {
            if (mode == RescanMode.valueOf(stringMode)) {
                return true;
            }
        } catch (IllegalArgumentException ex) {
            //do nothing, illegal argument
        }
        return false;

    }


    public void txPublished(final String error) {
        super.txPublished(error);
        final Context app = BreadApp.getBreadContext();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (app instanceof Activity)
                    BRAnimator.showBreadSignal((Activity) app, Utils.isNullOrEmpty(error) ? app.getString(R.string.Alerts_sendSuccess) : app.getString(R.string.Alert_error),
                            Utils.isNullOrEmpty(error) ? app.getString(R.string.Alerts_sendSuccessSubheader) : "Error: " + error, Utils.isNullOrEmpty(error) ? R.drawable.ic_check_mark_white : R.drawable.ic_error_outline_black_24dp, new BROnSignalCompletion() {
                                @Override
                                public void onComplete() {
                                    if (!((Activity) app).isDestroyed())
                                        ((Activity) app).getFragmentManager().popBackStack();
                                }
                            });

            }
        });

    }

    public void saveBlocks(boolean replace, BRCoreMerkleBlock[] blocks) {
        super.saveBlocks(replace, blocks);

        Context app = BreadApp.getBreadContext();
        if (app == null) return;
        if (replace) MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, getIso());
        BlockEntity[] entities = new BlockEntity[blocks.length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new BlockEntity(blocks[i].serialize(), (int) blocks[i].getHeight());
        }

        MerkleBlockDataSource.getInstance(app).putMerkleBlocks(app, getIso(), entities);
    }

    public void savePeers(boolean replace, BRCorePeer[] peers) {
        super.savePeers(replace, peers);
        Context app = BreadApp.getBreadContext();
        if (app == null) return;
        if (replace) PeerDataSource.getInstance(app).deleteAllPeers(app, getIso());
        PeerEntity[] entities = new PeerEntity[peers.length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new PeerEntity(peers[i].getAddress(), TypesConverter.intToBytes(peers[i].getPort()), TypesConverter.long2byteArray(peers[i].getTimestamp()));
        }
        PeerDataSource.getInstance(app).putPeers(app, getIso(), entities);

    }

    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    public BRCoreTransaction[] loadTransactions() {
        Context app = BreadApp.getBreadContext();

        List<BRTransactionEntity> txs = BtcBchTransactionDataStore.getInstance(app).getAllTransactions(app, getIso());
        if (txs == null || txs.size() == 0) return new BRCoreTransaction[0];
        BRCoreTransaction arr[] = new BRCoreTransaction[txs.size()];
        for (int i = 0; i < txs.size(); i++) {
            BRTransactionEntity ent = txs.get(i);
            arr[i] = new BRCoreTransaction(ent.getBuff(), ent.getBlockheight(), ent.getTimestamp());
        }
        return arr;
    }

    public BRCoreMerkleBlock[] loadBlocks() {
        Context app = BreadApp.getBreadContext();
        List<BRMerkleBlockEntity> blocks = MerkleBlockDataSource.getInstance(app).getAllMerkleBlocks(app, getIso());
        if (blocks == null || blocks.size() == 0) return new BRCoreMerkleBlock[0];
        BRCoreMerkleBlock arr[] = new BRCoreMerkleBlock[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            BRMerkleBlockEntity ent = blocks.get(i);
            arr[i] = new BRCoreMerkleBlock(ent.getBuff(), ent.getBlockHeight());
        }
        return arr;
    }

    public BRCorePeer[] loadPeers() {
        Context app = BreadApp.getBreadContext();
        List<BRPeerEntity> peers = PeerDataSource.getInstance(app).getAllPeers(app, getIso());
        if (peers == null || peers.size() == 0) return new BRCorePeer[0];
        BRCorePeer arr[] = new BRCorePeer[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            BRPeerEntity ent = peers.get(i);
            arr[i] = new BRCorePeer(ent.getAddress(), TypesConverter.bytesToInt(ent.getPort()), TypesConverter.byteArray2long(ent.getTimeStamp()));
        }
        return arr;
    }

    @Override
    public int getForkId() {
        return super.getForkId();
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        mWalletManagerHelper.addBalanceChangedListener(listener);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        mWalletManagerHelper.onBalanceChanged(balance);
    }

//    @Override
//    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener listener) {
//        mWalletManagerHelper.addTxStatusUpdatedListener(listener);
//    }

    @Override
    public void addSyncListener(SyncListener listener) {
        mWalletManagerHelper.addSyncListener(listener);
    }

    @Override
    public void removeSyncListener(SyncListener listener) {
        mWalletManagerHelper.removeSyncListener(listener);
    }

    public void onSyncStarted() {
        mWalletManagerHelper.onSyncStarted();
    }

    public void onSyncStopped(String error) {
        mWalletManagerHelper.onSyncStopped(error);
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified listener) {
        mWalletManagerHelper.addTxListModifiedListener(listener);
    }

    public void onTxListModified(String hash) {
        mWalletManagerHelper.onTxListModified(hash);
    }

    public void balanceChanged(final long balance) {
        super.balanceChanged(balance);
        final Context app = BreadApp.getBreadContext();
        setCachedBalance(app, new BigDecimal(balance));
        onBalanceChanged(new BigDecimal(balance));
        refreshAddress(app);
    }

    public void txStatusUpdate() {
        super.txStatusUpdate();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {

                // TODO This listener is never added... so this is not doing anything.
//                for (OnTxStatusUpdatedListener listener : txStatusUpdatedListeners)
//                    if (listener != null) listener.onTxStatusUpdated();
                onTxListModified(null);
            }
        });

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long blockHeight = getPeerManager().getLastBlockHeight();

                final Context context = BreadApp.getBreadContext();
                if (context != null) {
                    BRSharedPrefs.putLastBlockHeight(context, getIso(), (int) blockHeight);
                }
            }
        });
    }

    public void syncStarted() {
        super.syncStarted();
        MyLog.d( "syncStarted: ");
        final Context app = BreadApp.getBreadContext();
        if (Utils.isEmulatorOrDebug(app)) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(app, "syncStarted " + getIso(), Toast.LENGTH_LONG).show();
                }
            });
        }

        onSyncStarted();
    }

    protected abstract void syncStopped(Context context);

    public void syncStopped(final String error) {
        super.syncStopped(error);
        MyLog.d( "syncStopped: " + error);
        final Context context = BreadApp.getBreadContext();
        if (Utils.isNullOrEmpty(error)) {
            BRSharedPrefs.putAllowSpend(context, getIso(), true);
            syncStopped(context);
        }

        onSyncStopped(error);

        if (Utils.isEmulatorOrDebug(context))
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(context, "SyncStopped " + getIso() + " err(" + error + ") ", Toast.LENGTH_LONG).show();
                }
            });

        MyLog.e( "syncStopped: peerManager:" + getPeerManager().toString());

        if (!Utils.isNullOrEmpty(error)) {
            if (mSyncRetryCount < SYNC_MAX_RETRY) {
                MyLog.e( "syncStopped: Retrying: " + mSyncRetryCount);
                //Retry
                mSyncRetryCount++;
                BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        getPeerManager().connect();
                    }
                });

            } else {
                //Give up
                MyLog.e( "syncStopped: Giving up: " + mSyncRetryCount);
                mSyncRetryCount = 0;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(context, "Syncing failed, retried " + SYNC_MAX_RETRY + " times.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

    }

    public void onTxAdded(BRCoreTransaction transaction) {
        super.onTxAdded(transaction);
        final Context ctx = BreadApp.getBreadContext();
        final WalletsMaster master = WalletsMaster.getInstance(ctx);

        TxMetaData metaData = KVStoreManager.getInstance().createMetadata(ctx, this, new CryptoTransaction(transaction));
        KVStoreManager.getInstance().putTxMetaData(ctx, metaData, transaction.getHash());

        final long amount = getWallet().getTransactionAmount(transaction);
        if (amount > 0) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    String am = CurrencyUtils.getFormattedAmount(ctx, getIso(), getCryptoForSmallestCrypto(ctx, new BigDecimal(amount)));
                    BigDecimal bigAmount = master.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(amount), null);
                    String amCur = CurrencyUtils.getFormattedAmount(ctx, BRSharedPrefs.getPreferredFiatIso(ctx), bigAmount == null ? BigDecimal.ZERO : bigAmount);
                    String formatted = String.format("%s (%s)", am, amCur);
                    final String strToShow = String.format(ctx.getString(R.string.TransactionDetails_received), formatted);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!BRToast.isToastShown()) {
                                if (Utils.isEmulatorOrDebug(ctx))
                                    BRToast.showCustomToast(ctx, strToShow,
                                            BreadApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
                                AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                    final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                    if (mp != null) try {
                                        mp.start();
                                    } catch (IllegalArgumentException ex) {
                                        MyLog.e( "run: "+ ex);
                                    }
                                }
                                if (ctx instanceof Activity && BRSharedPrefs.getShowNotification(ctx))
                                    BRNotificationManager.sendNotification((Activity) ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), strToShow, 1);
                                else
                                    MyLog.e( "onTxAdded: ctx is not activity");
                            }
                        }
                    }, DateUtils.SECOND_IN_MILLIS);


                }
            });
        }
        if (ctx != null)
            TransactionStorageManager.putTransaction(ctx, getIso(), new BRTransactionEntity(transaction.serialize(), transaction.getBlockHeight(), transaction.getTimestamp(), BRCoreKey.encodeHex(transaction.getHash()), getIso()));
        else
            MyLog.e( "onTxAdded: ctx is null!");

        onTxListModified(transaction.getReverseHash());
    }

    public void onTxDeleted(final String hash, int notifyUser, int recommendRescan) {
        super.onTxDeleted(hash, notifyUser, recommendRescan);
        MyLog.e( "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
        final Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            if (recommendRescan != 0)
                BRSharedPrefs.putScanRecommended(ctx, getIso(), true);
            if (notifyUser != 0)
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        BRDialog.showSimpleDialog(ctx, "Transaction failed!", hash);
                    }
                });
            TransactionStorageManager.removeTransaction(ctx, getIso(), hash);
        } else {
            MyLog.e( "onTxDeleted: Failed! ctx is null");
        }
        onTxListModified(hash);
    }

    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        super.onTxUpdated(hash, blockHeight, timeStamp);
        MyLog.d( "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            TransactionStorageManager.updateTransaction(ctx, getIso(), new BRTransactionEntity(null, blockHeight, timeStamp, hash, getIso()));

        } else {
            MyLog.e( "onTxUpdated: Failed, ctx is null");
        }
        onTxListModified(hash);
    }

}
