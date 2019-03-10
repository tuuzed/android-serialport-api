package com.tuuzed.androidx.serialport.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

@IntDef(flag = true, value = {ReadOrWriteException.READ, ReadOrWriteException.WRITE})
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface ReadOrWriteException {
    int READ = 1;
    int WRITE = 2;
}
