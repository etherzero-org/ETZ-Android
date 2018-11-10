package com.etzwallet.wallet.util;

import android.content.Context;
import android.support.annotation.WorkerThread;

import com.etzwallet.BuildConfig;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.wallets.CryptoAddress;
import com.platform.APIClient;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/5/18.
 * Copyright (c) 2018 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class JsonRpcHelper {
//    private static final String BRD_ETH_RPC_ENDPOINT = BuildConfig.BITCOIN_TESTNET ? "/ethq/ropsten/proxy" : "/ethq/mainnet/proxy";
    private static final String BRD_ETH_TX_ENDPOINT = BuildConfig.BITCOIN_TESTNET ? "/ethq/ropsten/" : "/ethq/mainnet/";
    private static final String PROTOCOL = "https";
    public static final String METHOD = "method";
    public static final String JSONRPC = "jsonrpc";
    public static final String ETH_BALANCE = "eth_getBalance";
    public static final String LATEST = "latest";
    public static final String PARAMS = "params";
    public static final String ID = "id";
    public static final String RESULT = "result";
    public static final String ACCOUNT = "account";
    public static final String ETH_GAS_PRICE = "eth_gasPrice";
    public static final String ETH_ESTIMATE_GAS = "eth_estimateGas";
    public static final String ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction";
    public static final String ERROR = "error";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String HASH = "hash";
    public static final String TO = "to";
    public static final String FROM = "from";
    public static final String CONTRACT_ADDRESS = "contractAddress";
    public static final String ADDRESS = "address";
    public static final String VALUE = "value";
    public static final String GAS = "gas";
    public static final String GAS_PRICE = "gasPrice";
    public static final String NONCE = "nonce";
    public static final String GAS_USED = "gasUsed";
    public static final String BLOCK_NUMBER = "blockNumber";
    public static final String ETH_BLOCK_NUMBER = "eth_blockNumber";
    public static final String ETH_TRANSACTION_COUNT = "eth_getTransactionCount";
    public static final String BLOCK_HASH = "blockHash";
    public static final String LOG_INDEX = "logIndex";
    public static final String INPUT = "input";
    public static final String CONFIRMATIONS = "confirmations";
    public static final String TRANSACTION_INDEX = "transactionIndex";
    public static final String TIMESTAMP = "timeStamp";
    public static final String IS_ERROR = "isError";
    public static final String TOPICS = "topics";
    public static final String DATA = "data";
    public static final String TRANSACTION_HASH = "transactionHash";

    public static final String EASY_HOME = "easyetz.io/etzq/";
    public static final String VERSION_PATH = "easyetz.io/airdropapi/";

    private JsonRpcHelper() {
    }

    public interface JsonRpcRequestListener {

        void onRpcRequestCompleted(String jsonResult);
    }

    public static String getEthereumRpcUrl() {
//        return PROTOCOL + "://" + BreadApp.HOST + JsonRpcHelper.BRD_ETH_RPC_ENDPOINT;
        return PROTOCOL + "://" + "www.etzrpc.org:443";

    }

    //請求版本更新
    public static String versionCheekUrl(){
        return PROTOCOL + "://" + VERSION_PATH + "api/v1/versionCheck";
//        return "http://10.0.3.2:8080/api/v1/versionCheck";
        //模拟器 10.0.3.2  真机 10.0.2.2
    }


    //代币余额
    public static String createTokenTransactionsUrl(String address, String contractAddress) {
//        return PROTOCOL + "://" + BreadApp.HOST + BRD_ETH_TX_ENDPOINT + "query?" + "module=account&action=tokenbalance"
//                + "&address=" + address + "&contractaddress=" + contractAddress;

        return PROTOCOL + "://" + EASY_HOME + "api/v1/gettokenBlance?address=" + address + "&contractaddress=" + contractAddress;
//
    }

    //power 模拟器ip http://10.0.3.2:8080
//    public static String getPowerValue(CryptoAddress address) { return "http://10.0.3.2:8080/api/v1/getPower?address=" + address; }
    public static String getPowerValue(CryptoAddress address) { return PROTOCOL + "://" + EASY_HOME + "api/v1/getPower?address=" + address; }

    //代币列表

    public static String getTokenListUrl(){
        return "http://10.0.3.2:8080/api/v1/getTokenList";
    }


    //etz交易记录
    public static String createEthereumTransactionsUrl(String address) {
//        return PROTOCOL + "://" + BreadApp.HOST + BRD_ETH_TX_ENDPOINT
//                + "query?module=account&action=txlist&address=" + address;
        return PROTOCOL + "://" + EASY_HOME +"api/v1/getEtzTxlist?address="+address;
    }
    //代币交易记录
    public static String createLogsUrl(String address, String contract, String event) {

//        return PROTOCOL + "://" + EASY_HOME + "api/v1/gettokenLogs?"
//                + "module=logs&action=getLogs"
//                + "&fromBlock=0&toBlock=latest"
//                + (null == contract ? "" : ("&address=" + contract))
//                + "&topic0=" + event
//                + "&topic1=" + address
//                + "&topic1_2_opr=or"
//                + "&topic2=" + address;

//        https://openetz.org/etzq/api/v1/gettokenLogs?address=0x86d105D5FA67F3eEf986F75b7e63C6664f88319A&topic0=0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef&topic1=0x000000000000000000000000c158bae8a8bb67fb095eec9aa72e641004e2597e
        return PROTOCOL + "://" + EASY_HOME + "api/v1/gettokenLogs?" + (null == contract ? "" : ("address=" + contract+"&")) + "topic0=0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef" + "&topic1=" + address;
    }

    @WorkerThread
    public static void makeRpcRequestGet(Context app, String url, JSONObject payload, JsonRpcRequestListener listener) {

        Request request = new Request.Builder()
                .url(url)
//                .header("Content-Type", "application/json; charset=utf-8")
//                .header("Accept", "application/json")
                .method("GET",null)
                .build();
//                .get().build();


        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, true);

        String responseString;

        if(resp != null){
            responseString  = resp.getBodyText();
        }else{
            responseString = "";
        }

        if (listener != null) {
            listener.onRpcRequestCompleted(responseString);
        }

    }

    @WorkerThread
    public static void makeRpcRequest(Context app, String url, JSONObject payload, JsonRpcRequestListener listener) {
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .post(requestBody).build();


        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, true);
        String responseString;

        if(resp != null){
            responseString  = resp.getBodyText();
        }else{
            responseString = "";
        }

        if (listener != null) {
            listener.onRpcRequestCompleted(responseString);
        }

    }
}
