package com.etzwallet.presenter.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.etzwallet.BreadApp;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BRDialogView;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.ContactsEntity;
import com.etzwallet.presenter.entities.NodeEntity;
import com.etzwallet.tools.adapter.ListBaseAdapter;
import com.etzwallet.tools.animation.BRDialog;
import com.etzwallet.tools.manager.BRSharedPrefs;
import com.etzwallet.tools.threads.executor.BRExecutor;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.util.HttpUtils;
import com.etzwallet.wallet.util.JsonRpcHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ETZNodeSelectionActivity extends BRActivity {

    private BRText etz_node_text;
    private BRText etz_node_status;
    private ListView etz_node_lv;
    AlertDialog mDialog;
    ETZNodeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_etznode_selection);
        etz_node_text = findViewById(R.id.etz_node_text);
        etz_node_status = findViewById(R.id.etz_node_status);
        etz_node_lv = findViewById(R.id.etz_node_lv);

        etz_node_status.setText("已连接");
        adapter = new ETZNodeAdapter(this);
        adapter.setmList(getNodeList());
        etz_node_lv.setAdapter(adapter);
        etz_node_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NodeEntity item = adapter.getItem(position);
                etz_node_text.setText(item.getNode());
                BRSharedPrefs.putCurrentNode(ETZNodeSelectionActivity.this, item.getNode());
                JsonRpcHelper.node = item.getNode();
                adapter.notifyDataSetChanged();
            }
        });
        etz_node_lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                if (position > 2) {
                    final NodeEntity item = adapter.getItem(position);
                    String msg = getString(R.string.TokenList_remove) + item.getNode();
                    BRDialog.showCustomDialog(ETZNodeSelectionActivity.this, getString(R.string.NodeSelector_del_node),
                            msg, getString(R.string.TokenList_remove), getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    if (JsonRpcHelper.node.equals(item.getNode())) {
                                        String languageCode = Locale.getDefault().getLanguage();//手机语言
                                        JsonRpcHelper.node = languageCode.equalsIgnoreCase("zh") ? "http://47.90.101.201:9646" : "https://sg.etznumberone.com:443";
                                        BRSharedPrefs.putCurrentNode(BreadApp.getMyApp(), JsonRpcHelper.node);
                                    }
                                    adapter.removeItem(position);
                                    BRSharedPrefs.putNodeList(BreadApp.getBreadContext(), adapter.getList());
                                    brDialogView.dismiss();
                                }
                            }, new BRDialogView.BROnClickListener()

                            {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, 0);
                }
                return true;
            }
        });

        findViewById(R.id.node_back_button).

                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

        findViewById(R.id.node_add).

                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createDialog();
                    }
                });
    }

    class ETZNodeAdapter extends ListBaseAdapter<NodeEntity> {

        public ETZNodeAdapter(Context mContext) {
            super(mContext);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.node_list_item, null);
                new viewHolder(convertView);
            }
            viewHolder vh = (viewHolder) convertView.getTag();
            NodeEntity item = getItem(position);
            vh.node.setText(item.getNode());
            vh.address.setText(item.getNodeAddress());
            String node = BRSharedPrefs.getCurrentNode(ETZNodeSelectionActivity.this);
            if (item.getNode().equalsIgnoreCase(node)) {
                vh.img.setVisibility(View.VISIBLE);
            } else {
                vh.img.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    /**
     * 理财产品视图缓存类
     *
     * @author zhanfei
     */
    public class viewHolder {

        BRText address;
        BRText node;
        ImageView img;

        public viewHolder(View v) {
            address = v.findViewById(R.id.node_item_address);
            node = v.findViewById(R.id.node_item_node);
            img = v.findViewById(R.id.node_item_img);
            v.setTag(this);
        }

    }

    private List<NodeEntity> getNodeList() {

        List<NodeEntity> list;
        list = BRSharedPrefs.getNodeList(BreadApp.getBreadContext());
        if (list == null) {
            list = new ArrayList<>();
            list.add(new NodeEntity(getString(R.string.My_ETZ_node_hongkong), "http://47.90.101.201:9646"));
            list.add(new NodeEntity(getString(R.string.My_ETZ_node_usa), "https://usa.etznumberone.com:443"));
            list.add(new NodeEntity(getString(R.string.My_ETZ_node_singapore), "https://sg.etznumberone.com:443"));
            BRSharedPrefs.putNodeList(BreadApp.getBreadContext(), list);
        }

        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
        etz_node_text.setText(JsonRpcHelper.node);

    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(ETZNodeSelectionActivity.this);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(ETZNodeSelectionActivity.this, 32);
        int pad16 = Utils.getPixelsFromDps(ETZNodeSelectionActivity.this, 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.NodeSelector_enterTitle));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.NodeSelector_etzenterBody));

        final EditText input = new EditText(ETZNodeSelectionActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(ETZNodeSelectionActivity.this, 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        input.setText("http://");
        input.setSelection(input.getText().length());
        alertDialog.setView(input);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        mDialog = alertDialog.show();

        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = input.getText().toString().trim();
                if (!Utils.isNullOrEmpty(str) && str.contains("http") && Patterns.WEB_URL.matcher(str).matches()) {
                    ValidationOfTheNode(str);
                } else {
                    Toast.makeText(getApplication(), R.string.NodeSelector_error_node, Toast.LENGTH_LONG).show();
                }

            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                input.requestFocus();
                final InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(input, 0);
            }
        }, 200);
    }

    protected void ValidationOfTheNode(final String mnode) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethRpcUrl = mnode;
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.JSONRPC, "2.0");
                    payload.put(JsonRpcHelper.METHOD, "web3_clientVersion");
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                MyLog.i("ValidationOfTheNode=" + ethRpcUrl.toString());
                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {

                                JSONObject responseObject = new JSONObject(jsonResult);
                                MyLog.i("ValidationOfTheNode=" + responseObject.toString());

                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String result = responseObject.getString(JsonRpcHelper.RESULT);
                                    if (!Utils.isNullOrEmpty(result)) {
                                        List<NodeEntity> nList = adapter.getList();
                                        boolean isAdd = true;
                                        for (NodeEntity item : nList) {
                                            if (item.getNode().equals(mnode)) isAdd = false;
                                        }
                                        if (isAdd) {
                                            nList.add(new NodeEntity(getString(R.string.NodeSelector_Custom_rpc) + "(" + (nList.size() - 2) + ")", mnode));
                                            BRSharedPrefs.putNodeList(BreadApp.getBreadContext(), nList);
                                        }
                                        mDialog.dismiss();
                                    }
                                }
                                if (responseObject.has(JsonRpcHelper.ERROR)) {
                                    final String error = responseObject.getString(JsonRpcHelper.ERROR);
                                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(BreadApp.getBreadContext(), error, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }

                            } else {
                                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(BreadApp.getBreadContext(), getString(R.string.NodeSelector_link_dead), Toast.LENGTH_LONG).show();
                                    }
                                });

                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

                    }
                });
            }
        });
    }
}
