package com.etzwallet.presenter.activities;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.etzwallet.BuildConfig;
import com.etzwallet.R;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BREdit;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.tools.animation.BRAnimator;
import com.etzwallet.tools.manager.BRClipboardManager;
import com.etzwallet.tools.sqlite.BRSQLiteHelper;
import com.etzwallet.tools.sqlite.ETZContactsDataStore;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.tools.util.Utils;
import com.etzwallet.wallet.WalletsMaster;

public class ManageContactsActivity extends BRActivity implements View.OnClickListener {

    private LinearLayout contactsAdd;
    private LinearLayout contactsInfo;
    private Button done;
    private Button save;
    private Button cancel;
    private Button edit;
    private Button delete;
    private Button copy;
    private ImageButton scanBtn;
    private ImageButton back;
    private BRText title;
    private BRText tName;
    private BRText tRemarks;
    private BRText tAddress;
    private BREdit eName;
    private BREdit eRemarks;
    private BREdit eAddress;
    private int from = 0;//1.添加联系人2.显示联系人
    private static String savedMemo;
    private String caddress;
    private String cname;
    private String cremarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cintacts);
        contactsAdd = findViewById(R.id.contacts_manage_ll_add);
        contactsInfo = findViewById(R.id.contacts_manage_ll_show);
        done = findViewById(R.id.contacts_manage_done_btn);
        save = findViewById(R.id.contacts_manage_save_btn);
        cancel = findViewById(R.id.contacts_manage_cancel_btn);
        edit = findViewById(R.id.contacts_manage_edit_btn);
        delete = findViewById(R.id.contacts_manage_delete_btn);
        copy = findViewById(R.id.contacts_manage_copy_btn);
        scanBtn = findViewById(R.id.contacts_manage_scan_btn);
        back = findViewById(R.id.contacts_manage_back_btn);
        title = findViewById(R.id.contacts_manage_label);
        tName = findViewById(R.id.contacts_tv_name);
        tRemarks = findViewById(R.id.contacts_tv_description);
        tAddress = findViewById(R.id.contacts_tv_address);
        eName = findViewById(R.id.contacts_edit_name);
        eRemarks = findViewById(R.id.contacts_edit_description);
        eAddress = findViewById(R.id.contacts_edit_address);

        from = getIntent().getIntExtra("from", 0);
        if (from == 1) {
            contactsAdd.setVisibility(View.VISIBLE);
            contactsInfo.setVisibility(View.GONE);
            save.setVisibility(View.VISIBLE);
        } else if (from == 2) {
            cname = getIntent().getStringExtra("name");
            caddress = getIntent().getStringExtra("address");
            cremarks = getIntent().getStringExtra("remarks");
            tName.setText(cname);
            tAddress.setText(caddress);
            tRemarks.setText(cremarks);
            contactsAdd.setVisibility(View.GONE);
            contactsInfo.setVisibility(View.VISIBLE);
            edit.setVisibility(View.VISIBLE);
        } else {
        }
        done.setOnClickListener(this);
        save.setOnClickListener(this);
        cancel.setOnClickListener(this);
        edit.setOnClickListener(this);
        delete.setOnClickListener(this);
        copy.setOnClickListener(this);
        scanBtn.setOnClickListener(this);
        back.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.contacts_manage_save_btn:
                String address = eAddress.getText().toString().trim();
                String name = eName.getText().toString().trim();
                String remarks = eRemarks.getText().toString().trim();
                if (Utils.isNullOrEmpty(name))return;
                if (Utils.isNullOrEmpty(address))return;

                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.CONTACTS_NAME, name);
                values.put(BRSQLiteHelper.CONTACTS_WALLRT_ADDRESS, address);
                values.put(BRSQLiteHelper.CONTACTS_PHONE, "");
                values.put(BRSQLiteHelper.CONTACTS_REMARKS, remarks);
                boolean is = ETZContactsDataStore.getInstance(this).insertContacts(values);
                if (is){
                    Toast.makeText(this,"保存成功",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case R.id.contacts_manage_done_btn:
                contactsAdd.setVisibility(View.GONE);
                done.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                edit.setVisibility(View.VISIBLE);
                back.setVisibility(View.VISIBLE);
                contactsInfo.setVisibility(View.VISIBLE);
                String updataAddress = eAddress.getText().toString().trim();
                String updataName = eName.getText().toString().trim();
                String updataRemarks = eRemarks.getText().toString().trim();
                if (Utils.isNullOrEmpty(updataName))return;
                if (Utils.isNullOrEmpty(updataAddress))return;

                ContentValues updataDvalues = new ContentValues();
                updataDvalues.put(BRSQLiteHelper.CONTACTS_NAME, updataName);
                updataDvalues.put(BRSQLiteHelper.CONTACTS_WALLRT_ADDRESS, updataAddress);
                updataDvalues.put(BRSQLiteHelper.CONTACTS_PHONE, "");
                updataDvalues.put(BRSQLiteHelper.CONTACTS_REMARKS, updataRemarks);
                boolean updata = ETZContactsDataStore.getInstance(this).updataContacts(updataDvalues,caddress);
                if (updata){
                    tName.setText(updataName);
                    tAddress.setText(updataAddress);
                    tRemarks.setText(updataRemarks);
                }
                break;
            case R.id.contacts_manage_edit_btn:
                eName.setText(cname);
                eAddress.setText(caddress);
                eRemarks.setText(cremarks);
                contactsInfo.setVisibility(View.GONE);
                edit.setVisibility(View.GONE);
                back.setVisibility(View.GONE);
                done.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.VISIBLE);
                contactsAdd.setVisibility(View.VISIBLE);
                delete.setVisibility(View.VISIBLE);
                break;
            case R.id.contacts_manage_delete_btn:
               boolean del= ETZContactsDataStore.getInstance(this).deleteContacts(caddress);
               if (del){
                   Toast.makeText(this,"删除成功",Toast.LENGTH_LONG).show();
                   finish();
               }

                break;
            case R.id.contacts_manage_copy_btn:
                BRClipboardManager.putClipboard(ManageContactsActivity.this, tAddress.getText().toString());
                break;
            case R.id.contacts_manage_scan_btn:
                BRAnimator.openScanner(this, BRConstants.SCANNER_REQUEST);
                break;
            case R.id.contacts_manage_cancel_btn:
                contactsAdd.setVisibility(View.GONE);
                done.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                edit.setVisibility(View.VISIBLE);
                back.setVisibility(View.VISIBLE);
                contactsInfo.setVisibility(View.VISIBLE);
                break;
            case R.id.contacts_manage_back_btn:
                onBackPressed();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BRConstants.SCANNER_REQUEST) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra("result");
                int wen = result.indexOf("?");
                int aa=result.indexOf(":");
                if (wen > 0) {
                    result = result.substring(0, wen);
                } else if (aa>0){
                    result = result.substring(aa, result.length());
                }

                eAddress.setText(result);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }
}
