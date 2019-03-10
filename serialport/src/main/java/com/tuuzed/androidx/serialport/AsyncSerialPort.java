package com.tuuzed.androidx.serialport;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.tuuzed.androidx.serialport.annotation.DataBits;
import com.tuuzed.androidx.serialport.annotation.Parity;
import com.tuuzed.androidx.serialport.annotation.StopBits;
import com.tuuzed.androidx.serialport.internal.ByteBuf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AsyncSerialPort implements SerialPort {
    private static final String TAG = "AsyncSerialPort";
    private final HandlerThread mWriteThread = new HandlerThread("AsyncSerialPort-WriteThread");
    private final Thread mReadThread;
    private final NativeSerialPort mSerialPort;
    private final Handler mWriteHandler;
    private final ByteBuf mByteBuf;
    @Nullable
    private Listener mListener;
    private final InputStream mAsyncInputStream;
    private final OutputStream mAsyncOutputStream;

    public AsyncSerialPort(File device, int baudRate, @DataBits int dataBit, @StopBits int stopBit,
                           @Parity int parity) throws IOException, SecurityException {
        this(device, baudRate, dataBit, stopBit, parity, -1);
    }


    public AsyncSerialPort(File device, int baudRate, @DataBits int dataBit, @StopBits int stopBit,
                           @Parity int parity, int bufferCapacity) throws IOException, SecurityException {
        mSerialPort = new NativeSerialPort(device, baudRate, dataBit, stopBit, parity);
        mByteBuf = new ByteBuf(bufferCapacity);
        mWriteThread.start();
        mWriteHandler = new Handler(mWriteThread.getLooper());
        mAsyncInputStream = new AsyncInputStream();
        mAsyncOutputStream = new AsyncOutputStream();
        mReadThread = new Thread("AsyncSerialPort-ReadThread") {
            @Override
            public void run() {
                final InputStream in = mSerialPort.getInputStream();
                final byte[] buf = new byte[64];
                while (!mReadThread.isInterrupted()) {
                    try {
                        int len = in.read(buf);
                        final Listener listener = mListener;
                        if (listener != null) {
                            listener.onRead(AsyncSerialPort.this, buf, len);
                        }
                        mByteBuf.writeBytes(buf, len);
                    } catch (IOException e) {
                        Log.e(TAG, "run: read exception", e);
                        final Listener listener = mListener;
                        if (listener != null) {
                            listener.onReadException(AsyncSerialPort.this, e);
                        }
                    }
                }
            }
        };
        mReadThread.setName("");
        mReadThread.start();

    }

    @NonNull
    public AsyncSerialPort setListener(Listener listener) {
        mListener = listener;
        return this;
    }

    public InputStream getInputStream() {
        return mAsyncInputStream;
    }

    public OutputStream getOutputStream() {
        return mAsyncOutputStream;
    }

    public void writeBytes(@NonNull final byte[] data, final boolean flush) {
        mWriteHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream out = mSerialPort.getOutputStream();
                    out.write(data);
                    if (flush) {
                        out.flush();
                    }
                    final Listener listener = mListener;
                    if (listener != null) {
                        listener.onWrite(AsyncSerialPort.this, data);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "run: write exception", e);
                    final Listener listener = mListener;
                    if (listener != null) {
                        listener.onWriteException(AsyncSerialPort.this, data, e);
                    }
                }
            }
        });
    }

    public int readBytes(@NonNull byte[] dst) {
        return mByteBuf.readBytes(dst);
    }

    public synchronized boolean isOpen() {
        return mSerialPort.isOpen() && !mReadThread.isInterrupted();
    }

    public void shutdown() {
        mReadThread.interrupt();
        mSerialPort.shutdown();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mWriteThread.quitSafely();
        } else {
            mWriteThread.quit();
        }
    }


    public abstract class Listener {
        void onRead(@NonNull AsyncSerialPort serialPort, @NonNull byte[] data, int len) {

        }

        void onWrite(@NonNull AsyncSerialPort serialPort, @NonNull byte[] data) {

        }

        void onReadException(@NonNull AsyncSerialPort serialPort, @NonNull Throwable tr) {

        }

        void onWriteException(@NonNull AsyncSerialPort serialPort, @NonNull byte[] originalData, @NonNull Throwable tr) {

        }
    }


    private final class AsyncInputStream extends InputStream {
        private final byte[] read = new byte[1];

        @Override
        public int read() throws IOException {
            int i = readBytes(read);
            if (i == 1) {
                return read[0];
            }
            return -1;
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return readBytes(b);
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            byte[] src = new byte[len];
            int readLen = readBytes(src);
            System.arraycopy(src, 0, b, off, readLen);
            return readLen;
        }

        @Override
        public long skip(long n) throws IOException {
            long skipCount = 0;
            do {
                int c = (int) Math.min(n, 1024);
                byte[] skipBytes = new byte[c];
                skipCount += readBytes(skipBytes);
                n += c;
            } while (n > 0);
            return skipCount;
        }

        @Override
        public int available() throws IOException {
            return mByteBuf.size();
        }

    }

    private final class AsyncOutputStream extends OutputStream {
        private final byte[] write = new byte[1];
        private final byte[] empty = new byte[0];

        @Override
        public void write(int b) throws IOException {
            write[0] = (byte) b;
            writeBytes(write, false);
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            writeBytes(b, false);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            byte[] dest = new byte[len];
            System.arraycopy(b, off, dest, 0, len);
            writeBytes(dest, false);
        }

        @Override
        public void flush() throws IOException {
            writeBytes(empty, true);
        }
    }

}
