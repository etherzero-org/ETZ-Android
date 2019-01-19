package com.etzwallet.presenter.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.CurrencySettingsActivity;
import com.etzwallet.presenter.activities.ETZNodeSelectionActivity;
import com.etzwallet.presenter.entities.BRSettingsItem;
import com.etzwallet.tools.adapter.SettingsAdapter;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.wallet.util.JsonRpcHelper;
import com.etzwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import java.util.ArrayList;
import java.util.List;

public class AdvancedActivity extends BaseSettingsActivity {
    private static final String TAG = AdvancedActivity.class.getName();
    private ListView listView;
    public List<BRSettingsItem> items;
    public static boolean appVisible = false;
    private static AdvancedActivity app;

    public static AdvancedActivity getApp() {
        return app;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listView = findViewById(R.id.settings_list);

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_advanced;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        if (items == null)
            items = new ArrayList<>();
        items.clear();

        populateItems();
//        listView.addFooterView(new View(this), null, true);
//        listView.addHeaderView(new View(this), null, true);
        listView.setAdapter(new SettingsAdapter(this, R.layout.settings_list_item, items));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void populateItems() {

        items.add(new BRSettingsItem(getString(R.string.NodeSelector_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdvancedActivity.this, NodesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.empty_300);

            }
        }, false, R.drawable.chevron_right_light));
        items.add(new BRSettingsItem(getString(R.string.Settings_currency), BRSharedPrefs.getPreferredFiatIso(getApplication()), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdvancedActivity.this, DisplayCurrencyActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));
        String shareAddOn = BRSharedPrefs.getShareData(AdvancedActivity.this) ? getString(R.string.PushNotifications_on) : getString(R.string.PushNotifications_off);
        items.add(new BRSettingsItem(getString(R.string.Settings_shareData), shareAddOn, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdvancedActivity.this, ShareDataActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        items.add(new BRSettingsItem(getString(R.string.Settings_currencySettings), "", null, true, 0));

        final WalletBitcoinManager btcWallet = WalletBitcoinManager.getInstance(AdvancedActivity.this);
        if (btcWallet.getSettingsConfiguration().mSettingList.size() > 0)
            items.add(new BRSettingsItem(btcWallet.getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AdvancedActivity.this, CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(AdvancedActivity.this, btcWallet.getIso()); //change the current wallet to the one they enter settings to
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));
        final WalletEthManager ethWallet = WalletEthManager.getInstance(AdvancedActivity.this);
        if (ethWallet.getSettingsConfiguration().mSettingList.size() > 0)
            items.add(new BRSettingsItem(WalletEthManager.getInstance(AdvancedActivity.this).getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AdvancedActivity.this, CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(AdvancedActivity.this, ethWallet.getIso());//change the current wallet to the one they enter settings to
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }
}
