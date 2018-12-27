package com.etzwallet.presenter.fragments;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.etzwallet.R;
import com.etzwallet.presenter.activities.ContactsActivity;
import com.etzwallet.presenter.activities.CurrencySettingsActivity;
import com.etzwallet.presenter.activities.UpdatePinActivity;
import com.etzwallet.presenter.activities.settings.AboutActivity;
import com.etzwallet.presenter.activities.settings.AdvancedActivity;
import com.etzwallet.presenter.activities.settings.DisplayCurrencyActivity;
import com.etzwallet.presenter.activities.settings.SecurityCenterActivity;
import com.etzwallet.presenter.activities.settings.ShareDataActivity;
import com.etzwallet.presenter.activities.settings.UnlinkActivity;
import com.etzwallet.presenter.entities.BRSettingsItem;
import com.etzwallet.tools.adapter.SettingsAdapter;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import java.util.ArrayList;
import java.util.List;

public class FragmentMy extends Fragment {
    private ListView listView;
    public List<BRSettingsItem> items;
    public static boolean appVisible = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_settings, container, false);
        listView =rootView. findViewById(R.id.settings_list);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appVisible = true;
        if (items == null)
            items = new ArrayList<>();
        items.clear();

        populateItems();
        listView.addFooterView(new View(getActivity()), null, true);
        listView.setAdapter(new SettingsAdapter(getActivity(), R.layout.settings_list_item, items));
    }
    private void populateItems() {

        items.add(new BRSettingsItem(getString(R.string.Settings_wallet), "", null, true, 0));
        //钱包管理
        items.add(new BRSettingsItem(getString(R.string.My_contacts), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ContactsActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_wipe), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UnlinkActivity.class);
                startActivity(intent);
                getActivity(). overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_preferences), "", null, true, 0));

        items.add(new BRSettingsItem(getString(R.string.UpdatePin_updateTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UpdatePinActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        items.add(new BRSettingsItem(getString(R.string.Settings_currency), BRSharedPrefs.getPreferredFiatIso(getActivity()), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DisplayCurrencyActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_currencySettings), "", null, true, 0));

        final WalletBitcoinManager btcWallet = WalletBitcoinManager.getInstance(getActivity());
        if (btcWallet.getSettingsConfiguration().mSettingList.size() > 0)
            items.add(new BRSettingsItem(btcWallet.getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(getActivity(), btcWallet.getIso()); //change the current wallet to the one they enter settings to
                    startActivity(intent);
                    getActivity(). overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));
        final WalletEthManager ethWallet = WalletEthManager.getInstance(getActivity());
        if (ethWallet.getSettingsConfiguration().mSettingList.size() > 0)
            items.add(new BRSettingsItem(WalletEthManager.getInstance(getActivity()).getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(getActivity(), ethWallet.getIso());//change the current wallet to the one they enter settings to
                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_other), "", null, true, 0));

        String shareAddOn = BRSharedPrefs.getShareData(getActivity()) ? getString(R.string.PushNotifications_on) : getString(R.string.PushNotifications_off);

        items.add(new BRSettingsItem(getString(R.string.Settings_shareData), shareAddOn, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ShareDataActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.About_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        items.add(new BRSettingsItem(getString(R.string.Settings_advancedTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), AdvancedActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));
        items.add(new BRSettingsItem(getString(R.string.MenuButton_security), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SecurityCenterActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        }, false, R.drawable.chevron_right_light));


    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onPause() {
        super.onPause();
        appVisible = false;
    }


}