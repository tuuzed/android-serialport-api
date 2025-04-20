package android_serialport_api;

import android.util.Log;

import androidx.annotation.IntDef;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


public class SerialPort {
    private static final String TAG = "SerialPort";
    private static String suPath = null;

    // 校验位
    @IntDef(value = {Parity.NONE, Parity.ODD, Parity.EVEN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Parity {
        int NONE = 0;
        int ODD = 1;
        int EVEN = 2;
    }

    // 数据位
    @IntDef(value = {DataBit.B5, DataBit.B6, DataBit.B7, DataBit.B8})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DataBit {
        int B5 = 5;
        int B6 = 6;
        int B7 = 7;
        int B8 = 8;
    }

    // 停止位
    @IntDef(value = {StopBit.B1, StopBit.B2})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StopBit {
        int B1 = 1;
        int B2 = 2;
    }

    /**
     * Set the su binary path, the default su binary path is {@link #DEFAULT_SU_PATH}
     *
     * @param suPath su binary path
     */
    public static void setSuPath(String suPath) {
        SerialPort.suPath = suPath;
    }

    public static String getSuPath() {
        return suPath;
    }

    /**
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    @SuppressWarnings("FieldCanBeLocal")
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudRate) throws IOException {
        this(device, baudRate, 0);
    }

    public SerialPort(File file, int baudRate, int flags) throws IOException {
        this(file, baudRate, Parity.NONE, DataBit.B8, StopBit.B1, flags);
    }

    public SerialPort(File file, int baudRate, @Parity int parity, @DataBit int dataBits, @StopBit int stopBit) throws IOException {
        this(file, baudRate, parity, dataBits, stopBit, 0);
    }

    /**
     * 打开串口
     *
     * @param device   串口设备文件
     * @param baudRate 波特率
     * @param parity   奇偶校验，0 None（默认）； 1 Odd； 2 Even
     * @param dataBits 数据位，5 ~ 8 （默认 8）
     * @param stopBit  停止位，1 或 2 默认 1）
     * @param flags    标记 0（默认）
     * @throws IOException 串口打开失败时抛出
     */
    public SerialPort(File device, int baudRate, @Parity int parity, @DataBit int dataBits, @StopBit int stopBit, int flags) throws IOException {
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            Log.d(TAG, "Missing read/write permission, trying to chmod the file");
            try {
                /* Missing read/write permission, trying to chmod the file */
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                Process ps;
                if (suPath == null) {
                    ps = Runtime.getRuntime().exec(cmd);
                } else {
                    ps = Runtime.getRuntime().exec(suPath);
                    ps.getOutputStream().write(cmd.getBytes());
                }
                if ((ps.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new IOException("open serial port failure");
                }
            } catch (Exception e) {
                throw new IOException("open serial port failure", e);
            }
        }

        mFd = open(device.getAbsolutePath(), baudRate, parity, dataBits, stopBit, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException("open serial port failure");
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

    // JNI
    private native static FileDescriptor open(
            String path,
            int baudRate,
            int parity,
            int dataBits,
            int stopBit,
            int flags
    );

    public native void close();

    static {
        System.loadLibrary("serial-port");
    }
}
