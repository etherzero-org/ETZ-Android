package com.etzwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dapp.DappTransaction;
import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.HomeActivity;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.qrcode.QRUtils;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.wallets.ethereum.WalletEthManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Hash;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.github.novacrypto.bip44.M;

import static android.app.Activity.RESULT_OK;


public class FragmentDiscovery extends Fragment {
    private WebView web;
    private ProgressBar pb;
    private ImageView webHome;
    private ImageView web_refresh;
    private ImageButton web_back_up;
    private BRText webTitle;
    private BRText web_close;
    private EditText input;
    private ImageButton btn;
    private ImageButton web_scan;
    private RelativeLayout webTirle;
    private LinearLayout web_input_dapp;

    private String mFailingUrl = "https://dapp.easyetz.io/";
    private Map<String, Object> map = null;
    private ValueCallback<Uri> mUploadMessage;// 表单的数据信息
    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    private final static int FILECHOOSER_RESULTCODE = 1;// 表单的结果回调</span>
    private Uri imageUri;
    public static String tid = "";
    String languageCode;

    private Map<String, Object> mapf = null;
    public static FragmentDiscovery fd = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_web, container, false);
        web = rootView.findViewById(R.id.web_01);
        pb = rootView.findViewById(R.id.pb);
        webHome = rootView.findViewById(R.id.web_home);
        webTitle = rootView.findViewById(R.id.web_title);
        web_refresh = rootView.findViewById(R.id.web_refresh);
        input = rootView.findViewById(R.id.web_input);
        btn = rootView.findViewById(R.id.web_btn);
        web_scan = rootView.findViewById(R.id.web_scan);
        web_back_up = rootView.findViewById(R.id.web_back_up);
        web_close = rootView.findViewById(R.id.web_close);
        webTirle = rootView.findViewById(R.id.web_rl_title);
        web_input_dapp = rootView.findViewById(R.id.web_input_dapp);
        return rootView;
    }

    //https://easyetz.io/download_cn.html?Type=ANDROID&id=2
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fd = this;
        languageCode = Locale.getDefault().getLanguage();
        initView();
        webHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShowTitle();
                web.loadUrl("https://dapp.easyetz.io/");
            }
        });
        web_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShowTitle();
                web.loadUrl("https://dapp.easyetz.io/");
            }
        });
        web_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BRAnimator.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = input.getText().toString().trim();
                Utils.hideKeyboard(getActivity());
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    web.loadUrl(url);
                    web.evaluateJavascript("javascript:saveUrl('" + url + "')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }

                    });
                    isShowTab();
                } else {
                    Toast.makeText(getActivity(), R.string.Home_Discovery_error_url, Toast.LENGTH_LONG).show();
                }
            }
        });
        web_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                web.reload();

                QRUtils.share("sms:", getActivity(), "https://easyetz.io/download_cn.html?Type=ANDROID&id=2");
            }
        });
        web_back_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLastUrl();
                web.goBack();
            }
        });
    }

    void isShowTitle() {
        web_close.setVisibility(View.GONE);
        webHome.setVisibility(View.GONE);
        web_back_up.setVisibility(View.GONE);
        webTitle.setVisibility(View.GONE);
        web_input_dapp.setVisibility(View.VISIBLE);
        HomeActivity.getApp().isShowNavigationBar(true);
    }

    void isShowTab() {
        if (languageCode.equalsIgnoreCase("zh")) {
            web_close.setVisibility(View.VISIBLE);
            webHome.setVisibility(View.GONE);
        } else {
            web_close.setVisibility(View.GONE);
            webHome.setVisibility(View.VISIBLE);
        }
        web_back_up.setVisibility(View.VISIBLE);
        webTitle.setVisibility(View.VISIBLE);
        web_input_dapp.setVisibility(View.GONE);
        HomeActivity.getApp().isShowNavigationBar(false);
    }

    @SuppressLint({"NewApi", "SetJavaScriptEnabled"})
    @SuppressWarnings({"static-access", "deprecation"})
    protected void initView() {
        WebSettings ws = web.getSettings();
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setJavaScriptEnabled(true);
        ws.setSupportMultipleWindows(true);
        ws.setAllowFileAccess(true);
        ws.setBuiltInZoomControls(true);
        ws.setDomStorageEnabled(true);// 设置适应Html5的一些方法
        ws.setBlockNetworkImage(false);
        ws.setTextZoom(100);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 5.0以上手机播视频需设置
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        ws.setSupportZoom(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);

        if (Build.VERSION.SDK_INT < 19) {
            if (Build.VERSION.SDK_INT > 8) {
                ws.setPluginState(WebSettings.PluginState.ON);
            }
        }
        web.requestFocus();
        // 适应全屏 39适应竖屏 57适应横屏
        web.setInitialScale(39);
        web.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (Build.VERSION.SDK_INT >= 11)
            ws.setDisplayZoomControls(false);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(final WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    pb.setVisibility(View.GONE);
                } else {
                    pb.setVisibility(View.VISIBLE);
                    pb.setProgress(newProgress);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                mUploadCallbackAboveL = filePathCallback;
                take();
                return true;
            }

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                take();
            }

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType) {
                mUploadMessage = uploadMsg;
                take();
            }

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType, String capture) {
                mUploadMessage = uploadMsg;
                take();
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                return super.onCreateWindow(view, isDialog, isUserGesture,
                        resultMsg);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (!Utils.isNullOrEmpty(title) && title.toLowerCase().contains("error")) {
                    view.stopLoading();
                    view.clearAnimation();
                    web.loadUrl("file:///android_asset/anomalies/index.html");
                }
                if (!Utils.isNullOrEmpty(title) && !title.toLowerCase().contains("error")) {
                    webTitle.setText(title);
                }

//                super.onReceivedTitle(view, title);
            }

        });
        web.setOnKeyListener(new View.OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_BACK) && web.canGoBack()) {
                    myLastUrl();
                    web.goBack(); // goBack()表示返回WebView的上一页面
                    return true;
                }
                return false;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            private boolean bo;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                mFailingUrl = url;
                MyLog.i("weburl=" + url);
//                if (url.contains("dapp.easyetz.io")) {
                if (url.equalsIgnoreCase("https://dapp.easyetz.io/")) {
                    isShowTitle();

                } else {
                    isShowTab();
                }

                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (bo == true)
                    web.getSettings().setBlockNetworkImage(false);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return;
                }
                view.stopLoading();
                view.clearAnimation();
                web.getSettings().setBlockNetworkImage(false);
                mFailingUrl = failingUrl;
                web.loadUrl("file:///android_asset/anomalies/index.html");
            }

            // 新版本，只会在Android6及以上调用
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) { // 或者： if(request.getUrl().toString() .equals(getUrl()))
                    // 在这里显示自定义错误页
                    view.stopLoading();
                    view.clearAnimation();
                    web.getSettings().setBlockNetworkImage(false);
                    web.loadUrl("file:///android_asset/anomalies/index.html");
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
            }
        });

        web.addJavascriptInterface(new JsInterface(getActivity()), "easyetz");
        web.loadUrl("https://dapp.easyetz.io/");
//          web.loadUrl("http://127.0.0.1:3001");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * H5交互
     */
    public class JsInterface {
        Context ctx;

        public JsInterface(Context app) {
            this.ctx = app;
        }

        @JavascriptInterface
        public void etzTransaction(String strJson) {
            BRSharedPrefs.putlastDappHash(ctx, "");

            MyLog.i("etzTransaction----address=" + strJson);
            try {
                JSONObject jsonObject = new JSONObject(strJson);
                FragmentDiscovery.tid = jsonObject.optString("keyTime");
                BRSharedPrefs.putCurrentWalletIso(ctx, "etz");
                String value = jsonObject.optString("etzValue");
                if (!Utils.isNullOrEmpty(value)) {
                    value = new BigDecimal(value).toPlainString();
                } else {
                    value = "0";
                }
                MyLog.i("etzTransaction----address=" + value);
                Intent intent = new Intent(ctx, DappTransaction.class);
                intent.putExtra("to", jsonObject.optString("contractAddress"));
                intent.putExtra("value", value);
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

        /**
         * * Keccak-256 hash function that operates on a UTF-8 encoded String.
         *
         * @param utf8String UTF-8 encoded string
         * @return hash value as hex encoded string
         */
        @JavascriptInterface
        public String getSha3String(String utf8String) {
            Toast.makeText(BreadApp.getMyApp(), utf8String, Toast.LENGTH_LONG).show();
            return Hash.sha3String(utf8String);
        }

        @JavascriptInterface
        public String getBalance(String iso) {
            String balance = null;
            if (iso.equalsIgnoreCase("ETZ")) {
                balance = String.valueOf(BRSharedPrefs.getCachedBalance(ctx, iso).divide(new BigDecimal(WalletEthManager.ETHER_WEI)));
            } else {
                balance = String.valueOf(BRSharedPrefs.getCachedBalance(ctx, iso));
            }
            return balance;
        }

        @JavascriptInterface
        public void closeWeb() {
            ((Activity) ctx).finish();
        }

        @JavascriptInterface
        public String getTransactionHash() {
            String hash = BRSharedPrefs.getlastDappHash(BreadApp.getMyApp());
            return hash;
        }

        private void sayInvalidClipboardData(String title) {
            BRDialog.showCustomDialog(ctx, "", title,
                    ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismiss();
                        }
                    }, null, null, 0);
        }

        @JavascriptInterface
        public void errorReload() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (mFailingUrl != null) {
                        web.loadUrl(mFailingUrl);
                    }
                }
            });
        }

        /***
         * 横屏隐藏Title
         */
        @JavascriptInterface
        public void landscapeAndHideTitle() {
            MyLog.i("121212121212---landscapeAndHideTitle");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    webTirle.setVisibility(View.GONE);
                    Objects.requireNonNull(getActivity()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    Objects.requireNonNull(getActivity()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            });


        }

    }

    /**
     * 竖屏显示Title
     */
    public void portraitAndShowTitle() {
        Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        webTirle.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getActivity()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        webTirle.setVisibility(View.GONE);
//        Objects.requireNonNull(getActivity()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        Objects.requireNonNull(getActivity()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
            String hash = BRSharedPrefs.getlastDappHash(getActivity());

            if (!Utils.isNullOrEmpty(hash) && !Utils.isNullOrEmpty(tid)) {
                hash = hash.substring(2, hash.length());
                MyLog.i("hash**********=" + hash);
                web.evaluateJavascript("javascript:makeSaveData('" + hash + "','" + tid + "')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        MyLog.i("onReceiveValue-----" + value);
                    }

                });
            }
            web.resumeTimers();
        }
    }


    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (web != null) {
            web.onPause();
            web.pauseTimers();

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (web != null) {
            ViewParent parent = web.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(web);
            }
            web.removeAllViews();
            tid = "";
            web.destroy();
            web = null;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage && null == mUploadCallbackAboveL)
                return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data
                    .getData();
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (mUploadMessage != null) {

                if (result != null) {
                    String path = getPath(getContext(), result);
                    Uri uri = Uri.fromFile(new File(path));
                    mUploadMessage.onReceiveValue(uri);
                } else {
                    mUploadMessage.onReceiveValue(imageUri);
                }
                mUploadMessage = null;

            }
        }
    }

    /**
     * 拿到上一页的路径
     */
    private void myLastUrl() {
        WebBackForwardList backForwardList = web.copyBackForwardList();
        if (backForwardList != null && backForwardList.getSize() != 0) {
            //当前页面在历史队列中的位置
            int currentIndex = backForwardList.getCurrentIndex();
            WebHistoryItem historyItem =
                    backForwardList.getItemAtIndex(currentIndex - 1);
            if (historyItem != null) {
                String backPageUrl = historyItem.getUrl();
                MyLog.i("backPageUrl=" + backPageUrl);
//                if (backPageUrl.contains("dapp.easyetz.io")) {
                if (backPageUrl.equalsIgnoreCase("https://dapp.easyetz.io/") || backPageUrl.equalsIgnoreCase("https://dapp.easyetz.io/#/")) {
                    portraitAndShowTitle();
                    isShowTitle();

                } else {
                    isShowTab();
                }
                MyLog.i("weburl=========" + backPageUrl);
            }
        }
    }


    @SuppressWarnings("null")
    @TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
    private void onActivityResultAboveL(int requestCode, int resultCode,
                                        Intent data) {
        if (requestCode != FILECHOOSER_RESULTCODE
                || mUploadCallbackAboveL == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == RESULT_OK) {

            if (data == null) {

                results = new Uri[]{imageUri};
            } else {
                String dataString = data.getDataString();
                ClipData clipData = data.getClipData();

                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }

                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        if (results != null) {
            mUploadCallbackAboveL.onReceiveValue(results);
            mUploadCallbackAboveL = null;
        } else {
            results = new Uri[]{imageUri};
            mUploadCallbackAboveL.onReceiveValue(results);
            mUploadCallbackAboveL = null;
        }

        return;
    }

    private void take() {
        File imageStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyApp");
        // Create the storage directory if it does not exist
        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs();
        }
        File file = new File(imageStorageDir + File.separator + "IMG_"
                + String.valueOf(System.currentTimeMillis()) + ".jpg");
        imageUri = Uri.fromFile(file);

        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getActivity().getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(
                captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent i = new Intent(captureIntent);
            i.setComponent(new ComponentName(res.activityInfo.packageName,
                    res.activityInfo.name));
            i.setPackage(packageName);
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraIntents.add(i);

        }
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                cameraIntents.toArray(new Parcelable[]{}));
        startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();

        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    public void initUrl(String url) {
        if (web != null) {
            isShowTab();
            web.loadUrl(url);
        }
    }

}