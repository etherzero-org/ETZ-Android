package com.etzwallet.tools.sqlite;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.etzwallet.presenter.customviews.MyLog;
import com.etzwallet.presenter.entities.CurrencyEntity;
import com.etzwallet.presenter.entities.TransactionRecordEntity;
import com.etzwallet.tools.manager.BRReportsManager;
import com.etzwallet.tools.util.BRConstants;
import com.etzwallet.wallet.WalletsMaster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TransactionRecordDataSource implements BRDataSourceInterface {
    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.RECORD_BLOCKHASH,
            BRSQLiteHelper.RECORD_NUMBER,
            BRSQLiteHelper.RECORD_FROM,
            BRSQLiteHelper.RECORD_GAS,
            BRSQLiteHelper.RECORD_GASPRICE,
            BRSQLiteHelper.RECORD_HASH,
            BRSQLiteHelper.RECORD_INPUT,
            BRSQLiteHelper.RECORD_NONCE,
            BRSQLiteHelper.RECORD_TO,
            BRSQLiteHelper.RECORD_TRANSACTIONINDEX,
            BRSQLiteHelper.RECORD_VALUE,
            BRSQLiteHelper.RECORD_TIMESTAMP,
            BRSQLiteHelper.RECORD_GASUSED,
            BRSQLiteHelper.RECORD_CONTRACTADDRESS,
            BRSQLiteHelper.RECORD_STATUS,
            BRSQLiteHelper.RECORD_CONFIRMATIONS,
            BRSQLiteHelper.RECORD_ISERROR,
    };

    private static TransactionRecordDataSource instance;

    public static TransactionRecordDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionRecordDataSource(context);
        }
        return instance;
    }

    public TransactionRecordDataSource(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public void putCurrencies(Collection<TransactionRecordEntity> trList) {
        if (trList == null || trList.size() <= 0) {
            MyLog.e("putCurrencies: failed: " + trList.size());
            return;
        }

        try {
            database = openDatabase();
            database.beginTransaction();
            for (TransactionRecordEntity c : trList) {

                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.RECORD_BLOCKHASH, c.getBlockHash());
                values.put(BRSQLiteHelper.RECORD_NUMBER, c.getBlockNumber());
                values.put(BRSQLiteHelper.RECORD_FROM, c.getFrom());
                values.put(BRSQLiteHelper.RECORD_GAS, c.getGas());
                values.put(BRSQLiteHelper.RECORD_GASPRICE, c.getGasPrice());
                values.put(BRSQLiteHelper.RECORD_HASH, c.getHash());
                values.put(BRSQLiteHelper.RECORD_INPUT, c.getInput());
                values.put(BRSQLiteHelper.RECORD_NONCE, c.getNonce());
                values.put(BRSQLiteHelper.RECORD_TO, c.getTo());
                values.put(BRSQLiteHelper.RECORD_TRANSACTIONINDEX, c.getTransactionIndex());
                values.put(BRSQLiteHelper.RECORD_VALUE, c.getValue());
                values.put(BRSQLiteHelper.RECORD_TIMESTAMP, c.getTimestamp());
                values.put(BRSQLiteHelper.RECORD_GASUSED, c.getGasUsed());
                values.put(BRSQLiteHelper.RECORD_CONTRACTADDRESS, c.getContractAddress());
                values.put(BRSQLiteHelper.RECORD_STATUS, c.getStatus());
                values.put(BRSQLiteHelper.RECORD_CONFIRMATIONS, c.getConfirmations());
                values.put(BRSQLiteHelper.RECORD_ISERROR, c.getIsError());
                values.put(BRSQLiteHelper.RECORD_ISO, c.getIso());
                database.insertWithOnConflict(BRSQLiteHelper.TRANSACTION_RECORD_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            }
            database.setTransactionSuccessful();
        } catch (Exception ex) {
            MyLog.e("putCurrencies: failed: " + ex);
            BRReportsManager.reportBug(ex);

        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }


    public List<TransactionRecordEntity> getAllCurrencies(String iso) {

        List<TransactionRecordEntity> trList = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.TRANSACTION_RECORD_TABLE_NAME, allColumns, BRSQLiteHelper.RECORD_ISO + " = ? ",
                    new String[]{iso.toUpperCase()}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TransactionRecordEntity curEntity = cursorToRecord(cursor);
                trList.add(curEntity);
                cursor.moveToNext();
            }
            // make sure to close the cursor
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
        return trList;
    }

    private TransactionRecordEntity cursorToRecord(Cursor cursor) {
        return new TransactionRecordEntity(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8),
                cursor.getString(9), cursor.getString(10), cursor.getString(11), cursor.getString(12), cursor.getString(13),
                cursor.getString(14), cursor.getString(15), cursor.getString(16), cursor.getString(17));
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
    /**
     * 交易记录列表
     */
    public static final String TRANSACTION_RECORD_TABLE_NAME = "transactionRecordTable";
    public static final String RECORD_BLOCKHASH = "blockHash";
    public static final String RECORD_NUMBER = "blockNumber";
    public static final String RECORD_FROM = "from";
    public static final String RECORD_GAS = "gas";
    public static final String RECORD_GASPRICE = "gasPrice";
    public static final String RECORD_HASH = "hash";
    public static final String RECORD_INPUT = "input";
    public static final String RECORD_NONCE = "nonce";
    public static final String RECORD_TO = "to";
    public static final String RECORD_TRANSACTIONINDEX = "transactionIndex";
    public static final String RECORD_VALUE= "value";
    public static final String RECORD_TIMESTAMP = "timestamp";
    public static final String RECORD_GASUSED = "gasUsed";
    public static final String RECORD_CONTRACTADDRESS = "contractAddress";
    public static final String RECORD_STATUS = "status";
    public static final String RECORD_CONFIRMATIONS = "confirmations";
    public static final String RECORD_ISERROR = "isError";
    public static final String RECORD_ISO = "iso";

    private static final String TRANSACTION_RECORD_DATABASE_CREATE = "create table if not exists " + TRANSACTION_RECORD_TABLE_NAME + " (" +
            RECORD_BLOCKHASH + " text," +
            RECORD_NUMBER + " text," +
            RECORD_FROM + "text," +
            RECORD_GAS + " text," +
            RECORD_GASPRICE + " text," +
            RECORD_HASH + " text unique," +
            RECORD_INPUT + " text," +
            RECORD_NONCE + " text," +
            RECORD_TO + " text," +
            RECORD_TRANSACTIONINDEX + " text," +
            RECORD_VALUE + " text," +
            RECORD_TIMESTAMP + " text," +
            RECORD_GASUSED + " text," +
            RECORD_CONTRACTADDRESS + " text," +
            RECORD_STATUS + " text," +
            RECORD_CONFIRMATIONS + " text," +
            RECORD_ISERROR + " text, " +
            RECORD_ISO+" text,"+")" +
            ");";

}