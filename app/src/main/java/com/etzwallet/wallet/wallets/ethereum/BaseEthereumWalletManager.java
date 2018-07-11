package com.etzwallet.wallet.wallets.ethereum;

import android.content.Context;

import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.abstracts.OnBalanceChangedListener;
import com.etzwallet.wallet.abstracts.OnTxListModified;
import com.etzwallet.wallet.abstracts.SyncListener;
import com.etzwallet.wallet.wallets.WalletManagerHelper;

import java.math.BigDecimal;

public abstract class BaseEthereumWalletManager implements BaseWalletManager {

    private WalletManagerHelper mWalletManagerHelper;

    public BaseEthereumWalletManager() {
        mWalletManagerHelper = new WalletManagerHelper();
    }

    protected WalletManagerHelper getWalletManagerHelper() {
        return mWalletManagerHelper;
    }

    //TODO Not used by ETH, ERC20
    @Override
    public int getForkId() {
        return -1;
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        mWalletManagerHelper.addBalanceChangedListener(listener);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        mWalletManagerHelper.onBalanceChanged(balance);
    }

    // TODO not used by ETH, ERC20
    @Override
    public void addSyncListener(SyncListener listener) {
    }

    // TODO not used by ETH, ERC20
    @Override
    public void removeSyncListener(SyncListener listener) {
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified listener) {
        mWalletManagerHelper.addTxListModifiedListener(listener);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(), balance);
    }

}
