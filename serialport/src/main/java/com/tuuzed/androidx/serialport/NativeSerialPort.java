package com.tuuzed.androidx.serialport;

import android.util.Log;

import com.tuuzed.androidx.serialport.annotation.DataBits;
import com.tuuzed.androidx.serialport.annotation.Parity;
import com.tuuzed.androidx.serialport.annotation.StopBits;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;

public class NativeSerialPort implements SerialPort {
    private static final String TAG = "NativeSerialPort";

    private FileDescriptor mFd;
    private final FileInputStream mFileInputStream;
    private final FileOutputStream mFileOutputStream;

    public NativeSerialPort(@NonNull File device, int baudRate, @DataBits int dataBit,
                            @StopBits int stopBit, @Parity int parity
    ) throws IOException, SecurityException {
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
                        + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead()
                        || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                Log.e(TAG, "NativeSerialPort: ", e);
                throw new SecurityException();
            }
        }
        mFd = nativeOpen(device.getAbsolutePath(), baudRate, dataBit, stopBit, parity);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException("native open returns null");
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    @Override
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    @Override
    public synchronized boolean isOpen() {
        return mFd != null;
    }

    @Override
    public synchronized void shutdown() {
        if (mFileInputStream != null) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            nativeClose();
        } finally {
            mFd = null;
        }
    }

    // 调用JNI中 打开方法的声明
    private native static FileDescriptor nativeOpen(
            String path,
            int baudrate,
            int dataBit,
            int stopBit,
            int parity
    );

    private native void nativeClose();

    static {
        System.loadLibrary("native_serial_port");
    }
}