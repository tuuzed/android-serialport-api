package com.tuuzed.androidx.serialport.annotation;


import android.annotation.SuppressLint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

@IntDef(flag = true, value = {
        Parity.NONE,
        Parity.ODD,
        Parity.EVEN,
})
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags")
public @interface Parity {
    int NONE = 0;
    int ODD = 1;
    int EVEN = 2;
}
