package com.etzwallet.presenter.activities;

import android.app.Activity;
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
import com.etzwallet.tools.util.TokenUtil;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.APIClient;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.io.File;
import java.util.ArrayList;

public class AddWalletsActivity extends Activity {


    private AddTokenListAdapter mAdapter;
    private BREdit mSearchView;
    private RecyclerView mRecycler;
    private ImageButton mBackButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallets);
        TokenUtil.initialize(getApplicationContext());
        mRecycler = findViewById(R.id.token_list);
        mSearchView = findViewById(R.id.search_edit);
        mBackButton = findViewById(R.id.add_back_arrow);

        mAdapter = new AddTokenListAdapter(this, new AddTokenListAdapter.OnTokenAddOrRemovedListener() {
            @Override
            public void onTokenAdded(TokenItem token) {

                MyLog.i("onTokenAdded, -> " + token);

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
                MyLog.d("onTokenRemoved, -> " + token.name);

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
        findViewById(R.id.add_back_arrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter!=null)mAdapter.refreshList(getTokenList());
    }
    private ArrayList<TokenItem> getTokenList(){
        ArrayList<TokenItem> tokenItems = new ArrayList<>();
        TokenListMetaData md = KVStoreManager.getInstance().getTokenListMetaData(this);
        for (TokenItem tokenItem : TokenUtil.getTokenItems(this)) {
            MyLog.e(tokenItem.symbol);
            if (!md.isCurrencyEnabled(tokenItem.symbol)) {
                tokenItems.add(tokenItem);
            }
        }
        return tokenItems;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        WalletsMaster.getInstance(getApplication()).updateWallets(getApplication());
        super.onBackPressed();

    }
}
