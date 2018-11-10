package com.etzwallet.tools.util;

import android.annotation.TargetApi;
import android.graphics.Paint;
import android.os.Build;

/**
 * Created by byfieldj on 3/16/18.
 * <p>
 * This class checks if the device currently supports a given unicode symbol
 */

public class SymbolUtils {





    public SymbolUtils() {

    }


    @TargetApi(Build.VERSION_CODES.M)
    public boolean doesDeviceSupportSymbol(String unicodeString) {


        Paint paint = new Paint();

        if (paint.hasGlyph(unicodeString)) {

            return true;
        }


        return false;
    }
}
