package com.dapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.fragments.FragmentDiscovery;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import java.math.BigDecimal;

public class JsInterface {
    Context ctx;
    public JsInterface(Context app){
        this.ctx=app;
    }
    @JavascriptInterface
    public void etzTransaction(String address,String value, String data,String tid,String gasL,String gasP) {
//    public void etzTransaction(String json) {
        MyLog.i("etzTransaction----address="+address);
        MyLog.i("etzTransaction----value="+value);
        MyLog.i("etzTransaction----data="+data);
        MyLog.i("etzTransaction----tid="+tid);
        MyLog.i("etzTransaction----gasL="+gasL);
        MyLog.i("etzTransaction----gasP="+gasP);

        BRSharedPrefs.putlastDappHash(ctx,"");
        FragmentDiscovery.tid=tid;
//        if (Utils.isNullOrEmpty(data)||!data.startsWith("0x")){
//            sayInvalidClipboardData(ctx.getResources().getString(R.string.Dapp_dataError));
//            return;
//        }

        BRSharedPrefs.putCurrentWalletIso(ctx, "etz");
        Intent intent = new Intent(ctx, DappTransaction.class);
        intent.putExtra("to", address);
        intent.putExtra("value", value);
        intent.putExtra("data", data);
        intent.putExtra("gasL", gasL);
        intent.putExtra("gasP", gasP);
        ctx.startActivity(intent);
    }

    @JavascriptInterface
    public String getAddress() {
//        Toast.makeText(BreadApp.getMyApp(), WalletEthManager.getInstance(getApplication()).getAddress(), Toast.LENGTH_LONG).show();
        return WalletEthManager.getInstance(ctx).getAddress();
    }

    @JavascriptInterface
    public String getBalance(String iso) {
        String balance = null;
        if (iso.equalsIgnoreCase("ETZ")) {
            balance = String.valueOf(BRSharedPrefs.getCachedBalance(ctx, iso).divide(new BigDecimal(WalletEthManager.ETHER_WEI)));
        } else {
            balance = String.valueOf(BRSharedPrefs.getCachedBalance(ctx, iso));
        }

//        Toast.makeText(getApplicationContext(), balance, Toast.LENGTH_LONG).show();
        return balance;
    }

    @JavascriptInterface
    public void closeWeb() {
        ((Activity)ctx).finish();
    }
    @JavascriptInterface
    public String getTransactionHash() {
       String hash= BRSharedPrefs.getlastDappHash(BreadApp.getMyApp());
       return  hash;
    }

    private void sayInvalidClipboardData(String title) {
        BRDialog.showCustomDialog(ctx, "",title ,
                ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }
}
