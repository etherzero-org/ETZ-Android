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

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public class JsInterface {
    Context ctx;
    public JsInterface(Context app){
        this.ctx=app;
    }
    @JavascriptInterface
//    public void etzTransaction(String address,String value, String data,String tid,String gasL,String gasP) {
    public void etzTransaction(String strJson) {
        BRSharedPrefs.putlastDappHash(ctx,"");

        MyLog.i("etzTransaction----address="+strJson);
        try {
            JSONObject jsonObject=new JSONObject(strJson);
            FragmentDiscovery.tid=jsonObject.optString("keyTime");
            BRSharedPrefs.putCurrentWalletIso(ctx, "etz");
            Intent intent = new Intent(ctx, DappTransaction.class);
            intent.putExtra("to", jsonObject.optString("contractAddress"));
            intent.putExtra("value", jsonObject.optString("etzValue"));
            intent.putExtra("data", jsonObject.optString("datas"));
            intent.putExtra("gasL", jsonObject.optString("gasLimit"));
            intent.putExtra("gasP", jsonObject.optString("gasPrice"));
            ctx.startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }





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
