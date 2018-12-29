package com.etzwallet.presenter.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.etzwallet.R;
import com.etzwallet.presenter.activities.util.BRActivity;
import com.etzwallet.presenter.customviews.BRText;
import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.ContactsEntity;
import com.etzwallet.presenter.fragments.FragmentSend;
import com.etzwallet.tools.adapter.ListBaseAdapter;
import com.etzwallet.tools.sqlite.ETZContactsDataStore;

import java.util.List;

public class ContactsActivity extends BRActivity implements View.OnClickListener{
    private ListView lView;
    private ContactsAdapter adapter;
    public static final int ADD_CONTACTS=116;
    public static final int SHOW_CONTACTS=117;
    private int form=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        form=getIntent().getIntExtra("from",0);
        lView=findViewById(R.id.contacts_lv);
        findViewById(R.id.contacts_back_btn).setOnClickListener(this);
        findViewById(R.id.contacts_add_btn).setOnClickListener(this);
        adapter=new ContactsAdapter(this);
        lView.setAdapter(adapter);
        lView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ContactsEntity item= adapter.getItem(position);
                if (form==0) {
                    Intent intent = new Intent(ContactsActivity.this, ManageContactsActivity.class);
                    intent.putExtra("from", 2);
                    intent.putExtra("name", item.getCname());
                    intent.putExtra("address", item.getWalletAddress());
                    intent.putExtra("remarks", item.getRemarks());
                    startActivityForResult(intent, SHOW_CONTACTS);
                }else {
                    FragmentSend.ci.getContactsAddress(item.getWalletAddress());
                    finish();
                }
            }
        });
    }


    public void setListItem(){
       List<ContactsEntity>list= ETZContactsDataStore.getInstance(this).queryAllContacts();
        MyLog.i("ContactsActivity="+list.size());
        adapter.setmList(list);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.contacts_back_btn:
                onBackPressed();
                break;
            case R.id.contacts_add_btn:
                Intent intent=new Intent(this,ManageContactsActivity.class);
                intent.putExtra("from",1);
                startActivityForResult(intent,ADD_CONTACTS);

                break;
        }

    }

    class ContactsAdapter extends ListBaseAdapter<ContactsEntity>{

        public ContactsAdapter(Context mContext) {
            super(mContext);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.contacts_item, null);
                new viewHolder(convertView);
            }
            viewHolder vh = (viewHolder) convertView.getTag();
            ContactsEntity item=getItem(position);
            vh.name.setText(item.getCname());
            vh.address.setText(item.getWalletAddress());
            return convertView;
        }
    }

    /**
     * 理财产品视图缓存类
     *
     * @author zhanfei
     */
    public class viewHolder {

        BRText name;
        BRText address;

        public viewHolder(View v) {
            name = v.findViewById(R.id.contacts_item_name);
            address = v.findViewById(R.id.contacts_item_address);
            v.setTag(this);
        }

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case ADD_CONTACTS:
                break;
            case SHOW_CONTACTS:
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        setListItem();
    }
}
