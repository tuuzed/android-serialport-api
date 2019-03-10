package com.tuuzed.androidx.serialport.annotation;

import android.annotation.SuppressLint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

@IntDef(flag = true, value = {
        DataBits.DATA_BIT_5,
        DataBits.DATA_BIT_6,
        DataBits.DATA_BIT_7,
        DataBits.DATA_BIT_8,
})
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@SuppressLint("ShiftFlags")
public @interface DataBits {
    int DATA_BIT_5 = 5;
    int DATA_BIT_6 = 6;
    int DATA_BIT_7 = 7;
    int DATA_BIT_8 = 8;
}
