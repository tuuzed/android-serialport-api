package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SerialPort {
    private static final String TAG = "SerialPort";
    private static final String DEFAULT_SU_PATH = "/system/bin/su";
    private static String sSuPath = DEFAULT_SU_PATH;

    // 校验位
    public static final int PARITY_NONE = 0;
    public static final int PARITY_ODD = 1;
    public static final int PARITY_EVEN = 2;
    // 数据位
    public static final int DATABIT_5 = 5;
    public static final int DATABIT_6 = 6;
    public static final int DATABIT_7 = 7;
    public static final int DATABIT_8 = 8;
    // 停止位
    public static final int STOPBIT_1 = 1;
    public static final int STOPBIT_2 = 2;

    /**
     * Set the su binary path, the default su binary path is {@link #DEFAULT_SU_PATH}
     *
     * @param suPath su binary path
     */
    public static void setSuPath(String suPath) {
        if (suPath == null) {
            return;
        }
        sSuPath = suPath;
    }

    public static String getSuPath() {
        return sSuPath;
    }

    /**
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    @SuppressWarnings("FieldCanBeLocal")
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudRate) throws SecurityException, IOException {
        this(device, baudRate, 0);
    }

    public SerialPort(File file, int baudRate, int flags) throws SecurityException, IOException {
        this(file, baudRate, PARITY_NONE, DATABIT_8, STOPBIT_1, flags);
    }

    public SerialPort(File file, int baudRate, int parity, int dataBits, int stopBit) throws SecurityException, IOException {
        this(file, baudRate, parity, dataBits, stopBit, 0);
    }

    /**
     * 打开串口
     *
     * @param device   串口设备文件
     * @param baudRate 波特率
     * @param parity   奇偶校验，0 None（默认）； 1 Odd； 2 Even
     * @param dataBits 数据位，5 ~ 8  （默认8）
     * @param stopBit  停止位，1 或 2  （默认 1）
     * @param flags    标记 0（默认）
     * @throws SecurityException
     * @throws IOException
     */
    public SerialPort(File device, int baudRate, int parity, int dataBits, int stopBit, int flags) throws SecurityException, IOException {
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            Log.d(TAG, "Missing read/write permission, trying to chmod the file");
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec(sSuPath);
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = open(device.getAbsolutePath(), baudRate, parity, dataBits, stopBit, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
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
