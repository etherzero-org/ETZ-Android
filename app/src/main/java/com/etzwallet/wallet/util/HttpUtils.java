package com.etzwallet.wallet.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by jett on 2018/7/27.
 */

public class HttpUtils {
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
