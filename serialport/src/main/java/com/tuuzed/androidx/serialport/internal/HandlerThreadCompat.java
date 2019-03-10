package com.tuuzed.androidx.serialport.internal;

import android.os.Build;
import android.os.HandlerThread;

public class HandlerThreadCompat {

    public static void safeQuit(HandlerThread ht) {
        if (ht == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ht.quitSafely();
        } else {
            ht.quit();
        }
    }

}
