package com.etzwallet.presenter.customviews;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.etzwallet.R;

public class LoadingDialog extends Dialog {

    public LoadingDialog(Context context) {
        super(context, R.style.MyDialog);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.loading_dialog);

    }

    @Override
    public void show() {
        try {
            super.show();
        }catch (Exception e){
            MyLog.i("************"+e.getMessage());
        }

    }
}
