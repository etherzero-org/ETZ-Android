package com.etzwallet.tools.manager;

import android.content.Context;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.WorkerThread;

import com.etzwallet.BreadApp;
import com.etzwallet.presenter.activities.util.ActivityUTILS;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.CurrencyEntity;
import com.etzwallet.tools.sqlite.RatesDataSource;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;
import com.etzwallet.wallet.abstracts.BaseWalletManager;
import com.etzwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();

    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private Handler handler;

    public static final String HEADER_WALLET_ID = "X-Wallet-Id";
    public static final String HEADER_IS_INTERNAL = "X-Is-Internal";
    public static final String HEADER_TESTFLIGHT = "X-Testflight";
    public static final String HEADER_TESTNET = "X-Bitcoin-Testnet";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    @WorkerThread
    private void updateRates(Context context, String iso) {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = backupFetchRates(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = 0; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        if (tmpObj.getString("code").equalsIgnoreCase("BCH"))continue;
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = Float.valueOf(tmpObj.getString("rate"));
                        tmp.iso = iso;
                        MyLog.i("+++++++++++++++++++++++++++"+tmp.code+tmp.rate);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }

            } else {
                MyLog.e( "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (set.size() > 0) RatesDataSource.getInstance(context).putCurrencies(context, set);

    }



    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                updateData(context);
                            }
                        });
                    }
                });
            }
        };
    }

    @WorkerThread
    private void updateData(final Context context) {

        if (BreadApp.isAppInBackground(context)) {
            MyLog.e( "doInBackground: Stopping timer, no activity on.");
            stopTimerTask();
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateErc20Rates(context);
                updateETZRates(context);
//                updateTokenRates(context);
            }
        });

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                //get each wallet's rates
                updateRates(context,"BTC");

            }
        });

        List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(context).getAllWallets(context));

        for (final BaseWalletManager w : list) {
            //only update stuff for non erc20 for now, API endpoint BUG
            String iso=w.getIso();
            if (!Utils.isNullOrEmpty(iso)&&iso.equalsIgnoreCase("BTC") ) {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        w.updateFee(context);
                    }
                });
            }
        }

    }

    @WorkerThread
    private  synchronized  void updateETZRates(Context context){
        String url = "https://api.bddfinex.com/market/ticker?market=ETZBTC";
        String result = urlGET(context, url);
        try {
            if (Utils.isNullOrEmpty(result)) {
                MyLog.e( "updateErc20Rates: Failed to fetch");
                return;
            }
            Set<CurrencyEntity> tmp = new LinkedHashSet<>();
            JSONObject rJson= new JSONObject(result);
            JSONObject dJson=new JSONObject(rJson.getString("data"));

            String code = "BTC";
            String name = "Ether Zero";
            String iso = "ETZ";
            String rate = dJson.getString("last");
            CurrencyEntity ent = new CurrencyEntity(code, name, Float.valueOf(rate), iso);
            tmp.add(ent);
//            JSONObject json = new JSONObject(result);
//
//            JSONArray json1 = new JSONArray(json.getString("data"));
//            MyLog.i(result);
//            for (int i = 0; i <json1.length() ; i++) {
//                JSONObject json2 = new JSONObject(json1.getString(i));
//                String code = "BTC";
//                String name = json2.getString("name");
//                String iso = json2.getString("symbol");
//                JSONObject json3 = new JSONObject(json2.getString("quotes"));
//                JSONObject json4 = new JSONObject(json3.getString("BTC"));
//                String rate = json4.getString("price");
//
//                CurrencyEntity ent = new CurrencyEntity(code, name, Float.valueOf(rate), iso);
//                tmp.add(ent);
//            }
            RatesDataSource.getInstance(context).putCurrencies(context, tmp);
        } catch (JSONException e) {
            BRReportsManager.reportBug(e);
            e.printStackTrace();
        }
    }


    @WorkerThread
    private synchronized void updateErc20Rates(Context context) {
        //get all erc20 rates.
        String url = "https://api.coinmarketcap.com/v1/ticker/?limit=10&convert=BTC";
        String result = urlGET(context, url);

        MyLog.i( "updateErc20Rates: result==="+result);
        try {
            if (Utils.isNullOrEmpty(result)) {
                MyLog.e( "updateErc20Rates: Failed to fetch");
                return;
            }
            JSONArray arr = new JSONArray(result);
            if (arr.length() == 0) {
                MyLog.e( "updateErc20Rates: empty json");
                return;
            }
            Set<CurrencyEntity> tmp = new LinkedHashSet<>();
            for (int i = 0; i < arr.length(); i++) {

                Object obj = arr.get(i);
                JSONObject json = (JSONObject) obj;
                if (json.getString("symbol").equalsIgnoreCase("USDT")){
//                    BigDecimal btc=new BigDecimal(json.getString("price_btc"));
//                    BigDecimal usd=new BigDecimal(json.getString("price_usd"));
//                    String eashRate=btc.divide(usd,10,BigDecimal.ROUND_HALF_UP).toString();
                    String eashRate=json.getString("price_btc");
                    String code = "BTC";
                    String name = json.getString("name");
                    String rate = eashRate;
                    String iso = "EASH";
                    CurrencyEntity ent = new CurrencyEntity(code, name, Float.valueOf(rate), iso);
                    tmp.add(ent);
                    break;
                }




            }
            String code1 = "BTC";
            String name1 = "Black Options";
            String iso1 = "BO";
            String rate1 = "0";

            String code4 = "BTC";
            String name4 = "MSM";
            String iso4 = "MSM";
            String rate4 = "0";

            String code5 = "BTC";
            String name5 = "SQB";
            String iso5 = "SQB";
            String rate5 = "0";

            String code6 = "BTC";
            String name6 = "ABountifulCompany"; //全名
            String iso6 = "ABC";
            String rate6 = "0";


            CurrencyEntity ent1 = new CurrencyEntity(code1, name1, Float.valueOf(rate1), iso1);
            CurrencyEntity ent4 = new CurrencyEntity(code4, name4, Float.valueOf(rate4), iso4);
            CurrencyEntity ent5 = new CurrencyEntity(code5, name5, Float.valueOf(rate5), iso5);
            CurrencyEntity ent6 = new CurrencyEntity(code6, name6, Float.valueOf(rate6), iso6);

            tmp.add(ent1);
            tmp.add(ent4);
            tmp.add(ent5);
            tmp.add(ent6);

            RatesDataSource.getInstance(context).putCurrencies(context, tmp);
//            if (object != null)
//                BRReportsManager.reportBug(new IllegalArgumentException("JSONArray returns a wrong object: " + object));
        } catch (Exception e) {
            MyLog.i( "updateErc20Rates: error=="+e);
            BRReportsManager.reportBug(e);
            e.printStackTrace();
        }

    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();
        MyLog.e( "startTimer: started...");
        //initialize the TimerTask's job
        initializeTimerTask(context);

        timer.schedule(timerTask, 1000, 60000);
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

//    @WorkerThread
//    public static JSONArray fetchRates(Context app, BaseWalletManager walletManager) {
//        String url = "https://" + BreadApp.HOST + "/rates?currency=" + walletManager.getIso();
//        String jsonString = urlGET(app, url);
//        JSONArray jsonArray = null;
//        if (jsonString == null) {
//            MyLog.e( "fetchRates: failed, response is null");
//            return null;
//        }
//        try {
//            JSONObject obj = new JSONObject(jsonString);
//            jsonArray = obj.getJSONArray("body");
//
//        } catch (JSONException ignored) {
//        }
//        return jsonArray == null ? backupFetchRates(app, walletManager) : jsonArray;
//    }

    @WorkerThread
    public static JSONArray backupFetchRates(Context app) {
        String jsonString = urlGET(app, "https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    @WorkerThread
    public static String urlGET(Context app, String myURL) {
        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        String bodyText = null;
        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, false);


        try {
            if(resp != null){
                bodyText = resp.getBodyText();
                String strDate = resp.getHeaders().get("date");
                if (strDate == null) {
                    MyLog.e( "urlGET: strDate is null!");
                    return bodyText;
                }
                SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                Date date = formatter.parse(strDate);
                long timeStamp = date.getTime();
                BRSharedPrefs.putSecureTime(app, timeStamp);
            }else{
                bodyText = null;
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return bodyText;
    }

}
