package com.dapp;

import android.app.Activity;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BRButton;
import com.etzwallet.presenter.customviews.BREdit;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.CryptoRequest;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.manager.SendManager;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.util.JsonRpcHelper;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

public class DappTransaction extends BRActivity implements View.OnClickListener {

    private BRText d_title;
    private BRText d_to;
    private BRText d_value;
    private BRText d_data;
    private BREdit d_gasl;
    private BREdit d_gasp;
    private BRButton d_btn_send;
    private ImageButton back;
    private String to, vaule, data, gasL, gasP;
    private boolean isSend=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dapp_transaction);
        d_title = findViewById(R.id.dapp_t_title);
        d_to = findViewById(R.id.dapp_to);
        d_value = findViewById(R.id.dapp_value);
        d_gasl = findViewById(R.id.dapp_gasl);
        d_gasp = findViewById(R.id.dapp_gasp);
        d_data = findViewById(R.id.dapp_data);
        d_btn_send = findViewById(R.id.dapp_send_button);
        back = findViewById(R.id.dapp_back_btn);
        to = getIntent().getStringExtra("to");
        vaule = getIntent().getStringExtra("value");
        data = getIntent().getStringExtra("data");
        gasL = getIntent().getStringExtra("gasL");
        gasP = getIntent().getStringExtra("gasP");
        d_to.setText(to);
        BigDecimal bv = new BigDecimal(vaule);
        String strv = bv.divide(new BigDecimal(WalletEthManager.ETHER_WEI)).toPlainString();
        d_value.setText(strv);
        d_data.setText(data);
        //评估GasL
        getGasEstimate(to, vaule, data);
        d_gasl.addTextChangedListener(addET);
        d_gasp.addTextChangedListener(addET);
        d_btn_send.setOnClickListener(this);
        back.setOnClickListener(this);

    }

    private void getGasPrice() {
        final String[] gasPrice = {"18"};
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                MyLog.d("Making rpc request to -> " + ethUrl);
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();
                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_GAS_PRICE);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, "16");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        MyLog.d("jsonResult=: " + jsonResult);
                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                if (!Utils.isNullOrEmpty(jsonResult)) {
                                    JSONObject responseObject = new JSONObject(jsonResult);

                                    if (responseObject.has(JsonRpcHelper.RESULT)) {
                                        String gp = responseObject.getString(JsonRpcHelper.RESULT);
                                        String g = new BigInteger(gp.substring(2, gp.length()), 16).toString(10);
                                        gasPrice[0] = new BigDecimal(g).divide(new BigDecimal("1000000000")).toPlainString();
                                        MyLog.d("getGasPrice=: " + gasPrice[0]);
                                        DappTransaction.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (Utils.isNullOrEmpty(gasP)) {
                                                    d_gasp.setText(gasPrice[0]);
                                                } else {
                                                    d_gasp.setText(gasP);
                                                }
                                            }
                                        });

                                    }
                                } else {
                                    DappTransaction.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (Utils.isNullOrEmpty(gasP)) {
                                                d_gasp.setText(gasPrice[0]);
                                            } else {
                                                d_gasp.setText(gasP);
                                            }

                                        }
                                    });
                                }
                            } catch (JSONException je) {
                                je.printStackTrace();
                            }
                        } else {
                            DappTransaction.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (Utils.isNullOrEmpty(gasP)) {
                                        d_gasp.setText(gasPrice[0]);
                                    } else {
                                        d_gasp.setText(gasP);
                                    }

                                }
                            });
                        }
                    }
                });

            }

        });

    }

    private void getGasEstimate(final String to, final String amount, final String data) {
        final String[] gasEstimate = {"21000"};
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    JSONObject json = new JSONObject();
                    json.put("from", WalletEthManager.getInstance(DappTransaction.this).getAddress());
                    json.put("to", to);
                    if (Utils.isNullOrEmpty(amount) || new BigDecimal(amount) == BigDecimal.ZERO) {
                        json.put("value", "");
                    } else {
                        json.put("value", "0x" + new BigInteger(amount, 10).toString(16));
                    }

                    json.put("data", data);
                    MyLog.i("etzTransaction----address=" + data);
                    params.put(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_ESTIMATE_GAS);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, "0");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                MyLog.i("gasLimit=== " + payload.toString());
                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        MyLog.i("gasLimit=== " + jsonResult);
                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                JSONObject responseObject = new JSONObject(jsonResult);
                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    isSend=true;
                                    gasEstimate[0] = responseObject.getString(JsonRpcHelper.RESULT);
                                    MyLog.i("getGasEstimate: gasLimit==" + gasEstimate[0]);
                                    final String g = new BigInteger(gasEstimate[0].substring(2, gasEstimate[0].length()), 16).toString(10);
                                    DappTransaction.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            if (Long.valueOf(g) > 21000) {

                                                d_gasp.setText("2");
                                            } else {
                                                getGasPrice();
                                            }
                                            d_gasl.setText(g);
                                            MyLog.i("etzTransaction----address=" + g);
                                        }
                                    });

                                } else {
                                    isSend=false;
                                    MyLog.i("gas----------"+responseObject.getString(JsonRpcHelper.ERROR));
                                    BRDialog.showSimpleDialog(DappTransaction.this, getString(R.string.WipeWallet_failedTitle), responseObject.getString(JsonRpcHelper.ERROR));
//                                    DappTransaction.this.runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if (Utils.isNullOrEmpty(gasL)) {
//                                                d_gasl.setText(gasEstimate[0]);
//                                            } else {
//                                                d_gasl.setText(gasL);
//                                            }
//
//                                            getGasPrice();
//                                        }
//                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            isSend=false;
                            BRDialog.showSimpleDialog(DappTransaction.this, getString(R.string.WipeWallet_failedTitle), getString(R.string.gasFailed));
//                            DappTransaction.this.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (Utils.isNullOrEmpty(gasL)) {
//                                        d_gasl.setText(gasEstimate[0]);
//                                    } else {
//                                        d_gasl.setText(gasL);
//                                    }
//
//                                    getGasPrice();
//                                }
//                            });
                        }
                    }
                });
            }
        });
    }

    /**
     * 输入首字不能为0和.
     */
    private TextWatcher addET = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {

        }

        @Override
        public void afterTextChanged(Editable edt) {
            String temp = edt.toString();
            int posDot = temp.indexOf(".");
            int frist = temp.indexOf("0");
            if (frist == 0 || posDot == 0) {
                edt.delete(0, 1);
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dapp_send_button:
                //not allowed now
                if (!isSend){
                    //评估GasL
                    getGasEstimate(to, vaule, data);
                    return;
                }
                d_btn_send.setEnabled(false);
                WalletsMaster master = WalletsMaster.getInstance(getApplication());
                final BaseWalletManager wm = master.getCurrentWallet(getApplication());
                //get the current wallet used
                if (wm == null) {
                    MyLog.e("onClick: Wallet is null and it can't happen.");
                    BRReportsManager.reportBug(new NullPointerException("Wallet is null and it can't happen."), true);
                    return;
                }
                String rawAddress = d_to.getText().toString().trim();
                String value = d_value.getText().toString().trim();
                String gasL = d_gasl.getText().toString().trim();
                String gasP = d_gasp.getText().toString().trim();
                String data = d_data.getText().toString().trim();
                BigDecimal rawAmount = new BigDecimal(Utils.isNullOrEmpty(value) ? "0" : value);
                BigDecimal cryptoAmount = wm.getSmallestCryptoForCrypto(DappTransaction.this, rawAmount);
                final CryptoRequest item = new CryptoRequest(null, false, "", rawAddress, cryptoAmount, data, gasL, gasP);
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        SendManager.sendTransaction(DappTransaction.this, item, wm, null);

                    }
                });

                d_btn_send.setEnabled(true);
                break;
            case R.id.dapp_back_btn:
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
    }
}
