package com.tuuzed.androidx.serialport;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.tuuzed.androidx.serialport.annotation.DataBits;
import com.tuuzed.androidx.serialport.annotation.Parity;
import com.tuuzed.androidx.serialport.annotation.ReadOrWriteException;
import com.tuuzed.androidx.serialport.annotation.StopBits;
import com.tuuzed.androidx.serialport.internal.ByteBuf;
import com.tuuzed.androidx.serialport.internal.HandlerThreadCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AsyncSerialPort implements SerialPort {
    private static final String TAG = "AsyncSerialPort";

    private static final int MSG_WHAT_WRITE = 0x1000;
    private static final int MSG_WHAT_WRITE_AND_FLUSH = 0x1001;

    private final NativeSerialPort mSerialPort;
    private final HandlerThread mWriteThread = new HandlerThread("AsyncSerialPort-WriteThread");
    private final Thread mReadThread;
    private final Handler mWriteHandler;
    private final ByteBuf mByteBuf;
    private final InputStream mAsyncInputStream;
    private final OutputStream mAsyncOutputStream;
    private final AtomicBoolean mIsShutdown = new AtomicBoolean(false);
    @Nullable
    private Listener mListener;

    public AsyncSerialPort(@NonNull File device, int baudRate, @DataBits int dataBit, @StopBits int stopBit,
                           @Parity int parity) throws IOException, SecurityException {
        this(device, baudRate, dataBit, stopBit, parity, -1);
    }


    public AsyncSerialPort(@NonNull File device, int baudRate, @DataBits int dataBit, @StopBits int stopBit,
                           @Parity int parity, int bufferCapacity) throws IOException, SecurityException {
        mSerialPort = new NativeSerialPort(device, baudRate, dataBit, stopBit, parity);
        mByteBuf = new ByteBuf(bufferCapacity);
        mWriteThread.start();
        mWriteHandler = new Handler(mWriteThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleWriteBytesMessage(msg);
            }
        };
        mAsyncInputStream = new AsyncInputStream();
        mAsyncOutputStream = new AsyncOutputStream();
        mReadThread = new Thread("AsyncSerialPort-ReadThread") {
            @Override
            public void run() {
                loopReadBytes();
            }
        };
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

    public void writeBytes(@NonNull final byte[] data, final boolean flush) throws IOException {
        if (mIsShutdown.get()) {
            throw new IOException("Already shutdown!");
        }
        final Message message = new Message();
        if (flush) {
            message.what = MSG_WHAT_WRITE;
        } else {
            message.what = MSG_WHAT_WRITE_AND_FLUSH;
        }
        message.obj = data;
        mWriteHandler.sendMessage(message);
    }

    public int readBytes(@NonNull byte[] dst) throws IOException {
        if (mIsShutdown.get()) {
            throw new IOException("Already shutdown!");
        }
        return mByteBuf.readBytes(dst);
    }

    public boolean isOpen() {
        return mSerialPort.isOpen() && !mReadThread.isInterrupted();
    }

    public void shutdown() {
        mIsShutdown.set(true);
        HandlerThreadCompat.safeQuit(mWriteThread);
        mSerialPort.shutdown();
    }

    private void loopReadBytes() {
        final InputStream in = mSerialPort.getInputStream();
        final byte[] buf = new byte[64];
        while (!mIsShutdown.get()) {
            try {
                int len = in.read(buf);
                if (len > 0) {
                    final Listener listener = mListener;
                    if (listener != null) {
                        listener.onRead(AsyncSerialPort.this, buf, len);
                    }
                    mByteBuf.writeBytes(buf, len);
                }
            } catch (IOException e) {
                Log.e(TAG, "run: read exception", e);
                final Listener listener = mListener;
                if (listener != null) {
                    listener.onException(AsyncSerialPort.this, ReadOrWriteException.READ, e);
                }
            }
        }
    }

    private void handleWriteBytesMessage(Message msg) {
        if (msg == null) {
            return;
        }
        byte[] data;
        boolean flush;
        switch (msg.what) {
            case MSG_WHAT_WRITE:
                data = (byte[]) msg.obj;
                flush = false;
                break;
            case MSG_WHAT_WRITE_AND_FLUSH:
                data = (byte[]) msg.obj;
                flush = true;
                break;
            default:
                return;
        }
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
                listener.onException(AsyncSerialPort.this, ReadOrWriteException.WRITE, e);
            }
        }
    }

    public abstract class Listener {
        void onRead(@NonNull AsyncSerialPort serialPort, @NonNull byte[] data, int len) {
        }

        void onWrite(@NonNull AsyncSerialPort serialPort, @NonNull byte[] data) {
        }

        void onException(@NonNull AsyncSerialPort serialPort, @ReadOrWriteException int readOrWrite, @NonNull Throwable cause) {
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
