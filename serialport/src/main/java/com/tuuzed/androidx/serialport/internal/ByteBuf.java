package com.tuuzed.androidx.serialport.internal;

import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;

public final class ByteBuf {
    private static final String TAG = "ByteBuf";
    private final int capacity;
    private final CopyOnWriteArrayList<Byte> buffer = new CopyOnWriteArrayList<>();

    public ByteBuf() {
        this(-1);
    }

    public ByteBuf(int capacity) {
        this.capacity = capacity;
    }

    public void writeBytes(@NonNull byte[] data, int len) {
        // 判断容量是否超出
        if (data.length < len) {
            throw new IllegalArgumentException("data.length < len");
        }
        if (capacity > 0) {
            int discardCount = (buffer.size() + len) - capacity;
            if (discardCount > 0) {
                byte[] discardBytes = new byte[discardCount];
                readBytes(discardBytes);
                Log.w(TAG, "writeBytes: discardByte=" + Arrays.toString(discardBytes));
            }
        }
        for (int i = 0; i < len; i++) {
            buffer.add(data[i]);
        }
    }

    public int readBytes(@NonNull byte[] dst) {
        int size = 0;
        for (; size < dst.length; size++) {
            if (buffer.isEmpty()) {
                break;
            }
            dst[size] = buffer.remove(0);
        }
        return size;
    }


    public int size() {
        return buffer.size();
    }


}
