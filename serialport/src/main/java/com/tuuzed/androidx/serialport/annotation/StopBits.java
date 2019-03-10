package com.tuuzed.androidx.serialport.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

@IntDef(flag = true, value = {
        StopBits.STOP_BIT_1,
        StopBits.STOP_BIT_2
})
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface StopBits {
    int STOP_BIT_1 = 1;
    int STOP_BIT_2 = 2;
}
