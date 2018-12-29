package com.etzwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.core.ethereum.BREthereumToken;
import com.etzwallet.presenter.activities.settings.BaseSettingsActivity;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.TokenItem;
import com.etzwallet.tools.adapter.ManageTokenListAdapter;
import com.etzwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.etzwallet.tools.listeners.OnStartDragListener;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

public class ManageWalletsActivity extends BaseSettingsActivity implements OnStartDragListener {

    private static final String TAG = ManageWalletsActivity.class.getSimpleName();
    private ManageTokenListAdapter mAdapter;
    private RecyclerView mTokenList;
    private List<TokenListMetaData.TokenInfo> mTokens;
    private ItemTouchHelper mItemTouchHelper;


    @Override
    public int getLayoutId() {
        return R.layout.activity_manage_wallets;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTokenList = findViewById(R.id.token_list);

        mAdapter = new ManageTokenListAdapter(ManageWalletsActivity.this, new ManageTokenListAdapter.OnTokenShowOrHideListener() {
            @Override
            public void onShowToken(TokenItem token) {
                MyLog.d("onShowToken");

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);

                if (metaData.hiddenCurrencies == null)
                    metaData.hiddenCurrencies = new ArrayList<>();
                metaData.showCurrency(item.symbol);

                final TokenListMetaData finalMetaData = metaData;
                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, finalMetaData);
                mAdapter.notifyDataSetChanged();

            }

            @Override
            public void onHideToken(TokenItem token) {
                MyLog.d("onHideToken");

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);

                if (metaData.hiddenCurrencies == null)
                    metaData.hiddenCurrencies = new ArrayList<>();

                metaData.hiddenCurrencies.add(item);

                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, metaData);
                mAdapter.notifyDataSetChanged();

            }
        }, this);

        mTokenList.setLayoutManager(new LinearLayoutManager(ManageWalletsActivity.this));
        mTokenList.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mTokenList);
        findViewById(R.id.add_wallet_card).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //添加钱包
                Intent intent = new Intent(ManageWalletsActivity.this, AddWalletsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter!=null)mAdapter.refreshList(getTokenList());
    }

    private ArrayList<TokenItem> getTokenList(){
        final ArrayList<TokenItem> tokenItems = new ArrayList<>();
        TokenListMetaData tlMetaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
        if (tlMetaData == null || tlMetaData.enabledCurrencies.size() <= 0) {
            MyLog.e("数组为空  返回");
        } else {
            mTokens = tlMetaData.enabledCurrencies;

            for (int i = 0; i < mTokens.size(); i++) {

                TokenListMetaData.TokenInfo info = mTokens.get(i);
                TokenItem tokenItem = null;
                String tokenSymbol = mTokens.get(i).symbol;

//            if (!tokenSymbol.equalsIgnoreCase("btc") && !tokenSymbol.equalsIgnoreCase("bch") &&
//                    !tokenSymbol.equalsIgnoreCase("eth") && !tokenSymbol.equalsIgnoreCase("brd")) {
                if (!tokenSymbol.equalsIgnoreCase("btc") && !tokenSymbol.equalsIgnoreCase("etz") && !tokenSymbol.equalsIgnoreCase("eash")) {
                    BREthereumToken tk = WalletEthManager.getInstance(this).node.lookupToken(info.contractAddress);
                    if (tk == null) {
                        BRReportsManager.reportBug(new NullPointerException("No token for contract: " + info.contractAddress));
                    } else {
                        tokenItem = new TokenItem(tk.getAddress(), tk.getSymbol(), tk.getName(), null);
                    }
                } else if (tokenSymbol.equalsIgnoreCase("btc")) {
                    tokenItem = new TokenItem(null, "BTC", "Bitcoin", null);
                } else if (tokenSymbol.equalsIgnoreCase("etz")) {
                    tokenItem = new TokenItem(null, "ETZ", "EtherZero", "@drawable/etz");
                } else if (tokenSymbol.equalsIgnoreCase("eash")) {
                    tokenItem = new TokenItem(null, "EASH", "EASH", null);
                }


                if (tokenItem != null) {
                    tokenItems.add(tokenItem);
                }

            }

        }
        return tokenItems;
    }

    @Override
    protected void onPause() {
        super.onPause();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                WalletsMaster.getInstance(getApplication()).updateWallets(getApplication());
            }
        });

    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }
}
