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

public class SerialPort {
    private static final String TAG = "SerialPort";

    private FileDescriptor mFd;
    private final FileInputStream mFileInputStream;
    private final FileOutputStream mFileOutputStream;

    /***
     * 构造方法
     * @param device 串口文件
     * @param baudRate 波特率
     * @param dataBits 数据位
     * @param stopBits 停止位
     * @param parity   校验位
     * @throws SecurityException
     * @throws IOException
     */
    public SerialPort(
            File device,
            int baudRate,
            @DataBits int dataBits,
            @StopBits int stopBits,
            @Parity int parity
    ) throws SecurityException, IOException {
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
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudRate, dataBits, stopBits, parity);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException("native open returns null");
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    public synchronized boolean isOpen() {
        return mFd != null;
    }

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
            close();
        } finally {
            mFd = null;
        }
    }

    // 调用JNI中 打开方法的声明
    private native static FileDescriptor open(
            String path,
            int baudrate,
            int dataBits,
            int stopBits,
            int parity
    );

    private native void close();

    static {
        System.loadLibrary("serial_port");
    }
}