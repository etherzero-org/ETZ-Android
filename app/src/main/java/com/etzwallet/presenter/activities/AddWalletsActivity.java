package com.etzwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;

import com.etzwallet.R;
import com.etzwallet.core.ethereum.BREthereumToken;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BREdit;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.TokenItem;
import com.etzwallet.tools.adapter.AddTokenListAdapter;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;

public class AddWalletsActivity extends BRActivity {


    private BREthereumToken[] mTokens;
    private AddTokenListAdapter mAdapter;
    private BREdit mSearchView;
    private RecyclerView mRecycler;
    private static final String TAG = AddWalletsActivity.class.getSimpleName();
    private ImageButton mBackButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallets);

        mRecycler = findViewById(R.id.token_list);
        mSearchView = findViewById(R.id.search_edit);
        mBackButton = findViewById(R.id.back_arrow);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = mSearchView.getText().toString();

                if (mAdapter != null) {
                    mAdapter.filter(query);
                }

                if (query.equals("")) {
                    mAdapter.resetFilter();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();

        final ArrayList<TokenItem> tokenItems = new ArrayList<>();
        final TokenListMetaData md = KVStoreManager.getInstance().getTokenListMetaData(this);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mTokens = WalletEthManager.getInstance(AddWalletsActivity.this).node.tokens;

                for (BREthereumToken token : mTokens) {
                    MyLog.i( "run: token===="+token);
                    TokenItem tokenItem = new TokenItem(token.getAddress(), token.getSymbol(), token.getName(), null);

                    if (md!=null&&!md.isCurrencyEnabled(tokenItem.symbol))

                        tokenItems.add(tokenItem);
                }

            }
        });


        mAdapter = new AddTokenListAdapter(this, tokenItems, new AddTokenListAdapter.OnTokenAddOrRemovedListener() {
            @Override
            public void onTokenAdded(TokenItem token) {

                MyLog.i( "onTokenAdded, -> " + token);

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(AddWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);
                if (metaData.enabledCurrencies == null)
                    metaData.enabledCurrencies = new ArrayList<>();
                if (!metaData.isCurrencyEnabled(item.symbol))
                    metaData.enabledCurrencies.add(item);

                KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);

                mAdapter.notifyDataSetChanged();

            }

            @Override
            public void onTokenRemoved(TokenItem token) {
                MyLog.d( "onTokenRemoved, -> " + token.name);

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(AddWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);
                metaData.disableCurrency(item.symbol);

                KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);

                mAdapter.notifyDataSetChanged();
            }
        });
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        WalletsMaster.getInstance(this).updateWallets(this);
    }
}
