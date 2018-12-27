package com.etzwallet.tools.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.ContactsEntity;
import com.etzwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class ETZContactsDataStore implements BRDataSourceInterface {
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private static ETZContactsDataStore instance;
    public static final String[] allColumns = {
            BRSQLiteHelper.CONTACTS_NAME,
            BRSQLiteHelper.CONTACTS_WALLRT_ADDRESS,
            BRSQLiteHelper.CONTACTS_PHONE,
            BRSQLiteHelper.CONTACTS_REMARKS,
    };


    private ETZContactsDataStore(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static ETZContactsDataStore getInstance(Context context) {
        if (instance == null) {
            instance = new ETZContactsDataStore(context);
        }
        return instance;
    }

    /**
     * 增加联系人
     *
     * @param value
     * @return
     */
    public boolean insertContacts(ContentValues value) {
        database = openDatabase();
        Long sum = database.insert(BRSQLiteHelper.CONTACTS_TABLE_NAME, null, value);
        MyLog.i("database.insert"+sum);
        closeDatabase();
        if (sum > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取联系人列表
     *
     * @return
     */
    public List<ContactsEntity> queryAllContacts() {
        List<ContactsEntity> cList = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.CONTACTS_TABLE_NAME,
                    allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ContactsEntity entity = new ContactsEntity(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                cList.add(entity);
                cursor.moveToNext();
            }

        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
        }

        return cList;

    }

    /**
     * 删除联系人
     *
     * @param address
     * @return
     */
    public boolean deleteContacts(String address) {
        try {
            database = openDatabase();
            int num = database.delete(BRSQLiteHelper.CONTACTS_TABLE_NAME, BRSQLiteHelper.CONTACTS_WALLRT_ADDRESS + "=?", new String[]{address});
            MyLog.i("database.delete"+num);
            if (num > 0) {
                return true;
            }
        } finally {
            closeDatabase();
        }
        return false;

    }

    /**
     * 更新联系人
     * @param value
     * @param address
     * @return
     */
    public boolean updataContacts(ContentValues value, String address) {
        try {
            database = openDatabase();
            int r = database.update(BRSQLiteHelper.CONTACTS_TABLE_NAME, value, BRSQLiteHelper.CONTACTS_WALLRT_ADDRESS + "=?", new String[]{address});
            MyLog.i("database.update"+r);
            if (r > 0)
                return true;
        } finally {
            closeDatabase();
        }
        return false;
    }


    @Override
    public SQLiteDatabase openDatabase() {
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
        return database;
    }

    @Override
    public void closeDatabase() {
        database.close();
    }
}
