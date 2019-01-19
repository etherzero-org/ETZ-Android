package com.etzwallet.presenter.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.ManageWalletsActivity;
import com.etzwallet.presenter.activities.WalletActivity;
import com.etzwallet.presenter.activities.settings.SecurityCenterActivity;
import com.etzwallet.presenter.customviews.BRButton;
import com.etzwallet.presenter.customviews.BRNotificationBar;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.tools.adapter.WalletListAdapter;
import com.etzwallet.tools.listeners.RecyclerItemClickListener;
import com.etzwallet.tools.manager.BREventManager;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.manager.InternetManager;
import com.etzwallet.tools.manager.PromptManager;
import com.etzwallet.tools.sqlite.RatesDataSource;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.CurrencyUtils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;


public class FragmentWallet extends Fragment implements InternetManager.ConnectionReceiverListener, RatesDataSource.OnDataChanged {
    private static final String TAG = FragmentWallet.class.getName();
    private RecyclerView mWalletRecycler;
    public WalletListAdapter mAdapter;
    private BRText mFiatTotal;
    private RelativeLayout wallets_manage;
    //    private RelativeLayout mSupport;
    private PromptManager.PromptItem mCurrentPrompt;
    public BRNotificationBar mNotificationBar;

    private BRText mPromptTitle;
    private BRText mPromptDescription;
    private BRButton mPromptContinue;
    private BRButton mPromptDismiss;
    private BRButton close;
    private CardView mPromptCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.activity_home1, container, false);
        mWalletRecycler = rootView.findViewById(R.id.rv_wallet_list);
        mFiatTotal = rootView.findViewById(R.id.total_assets_usd);

        wallets_manage = rootView.findViewById(R.id.wallets_manage);
        mNotificationBar = rootView.findViewById(R.id.notification_bar);

        mPromptCard = rootView.findViewById(R.id.prompt_card);
        mPromptTitle = rootView.findViewById(R.id.prompt_title);
        mPromptDescription = rootView.findViewById(R.id.prompt_description);
        mPromptContinue = rootView.findViewById(R.id.continue_button);
        mPromptDismiss = rootView.findViewById(R.id.dismiss_button);
        close = rootView.findViewById(R.id.cancel_button);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWalletRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        populateWallets();
        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mAdapter.getItemCount() || position < 0) return;
                if (mAdapter.getItemViewType(position) == 0) {
                    //首页的钱包item 比特币 etz 代币等
                    BRSharedPrefs.putCurrentWalletIso(getActivity(), mAdapter.getItemAt(position).getIso());
                    Intent newIntent = new Intent(getActivity(), WalletActivity.class);
                    startActivity(newIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        wallets_manage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //添加钱包
                Intent intent = new Intent(getActivity(), ManageWalletsActivity.class);
                startActivity(intent);
//                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });


        mPromptDismiss.setColor(Color.parseColor("#b3c0c8"));
        mPromptDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePrompt();
            }
        });

        mPromptContinue.setColor(Color.parseColor("#4b77f3"));
        mPromptContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PromptManager.PromptInfo info = PromptManager.getInstance().promptInfo(getActivity(), mCurrentPrompt);
                if (info.listener != null)
                    info.listener.onClick(mPromptContinue);
                else
                    MyLog.e("Continue :" + info.title + " (FAILED)");
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNotificationBar();
            }
        });

    }

    public void hidePrompt() {
        mPromptCard.setVisibility(View.GONE);
        MyLog.e("hidePrompt: " + mCurrentPrompt);
//        if (mCurrentPrompt == PromptManager.PromptItem.SHARE_DATA) {
//            BRSharedPrefs.putPromptDismissed(app, "shareData", true);
//        }else
        if (mCurrentPrompt == PromptManager.PromptItem.FINGER_PRINT) {
            BRSharedPrefs.putPromptDismissed(getActivity(), "fingerprint", true);
        }
        if (mCurrentPrompt != null)
            BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(mCurrentPrompt) + ".dismissed");
        mCurrentPrompt = null;

    }

    @Override
    public void onResume() {
        super.onResume();
        showNextPromptIfNeeded();
        if (mAdapter != null) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    mAdapter.refreshList();
                }
            });

        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mAdapter != null)
                    mAdapter.startObserving();
            }
        }, DateUtils.SECOND_IN_MILLIS / 2);
        InternetManager.registerConnectionReceiver(getActivity(), this);
        updateUi();
        RatesDataSource.getInstance(getActivity()).addOnDataChangedListener(this);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BG:" + TAG + ":refreshBalances and address");
//                WalletsMaster.getInstance(getActivity()).refreshBalances(getActivity());
                WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity()).refreshAddress(getActivity());
            }
        });
        onConnectionChanged(InternetManager.getInstance().isConnected(getActivity()));
    }

    private void populateWallets() {
        mAdapter = new WalletListAdapter(getActivity());
        mWalletRecycler.setAdapter(mAdapter);

    }

    private void updateUi() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final BigDecimal fiatTotalAmount = WalletsMaster.getInstance(getActivity()).getAggregatedFiatBalance(getActivity());
                if (fiatTotalAmount == null) {
                    MyLog.e("updateUi: fiatTotalAmount is null");
                    return;
                }
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mFiatTotal.setText(CurrencyUtils.getFormattedAmount(BreadApp.getMyApp(), BRSharedPrefs.getPreferredFiatIso(BreadApp.getMyApp()), fiatTotalAmount));
                        if (mAdapter != null)
                            mAdapter.notifyDataSetChanged();
                    }
                });

            }
        });
    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(getActivity());
        MyLog.i("showNextPromptIfNeeded: toShow===" + toShow);
        if (toShow != null) {
            mCurrentPrompt = toShow;
//            MyLog.d( "showNextPrompt: " + toShow);
            PromptManager.PromptInfo promptInfo = PromptManager.getInstance().promptInfo(getActivity(), toShow);
            mPromptCard.setVisibility(View.VISIBLE);
            mPromptTitle.setText(promptInfo.title);
            mPromptDescription.setText(promptInfo.description);
            mPromptContinue.setOnClickListener(promptInfo.listener);

        } else {
            MyLog.i("showNextPrompt: nothing to show");
        }
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(getActivity(), this);
        mAdapter.stopObserving();
    }


    @Override
    public void onConnectionChanged(boolean isConnected) {
        MyLog.d("onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.INVISIBLE);
            }

            if (mAdapter != null) {
                mAdapter.startObserving();
            }
        } else {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.VISIBLE);
            }
        }
    }

    public void closeNotificationBar() {
        mNotificationBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onChanged() {
        updateUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}