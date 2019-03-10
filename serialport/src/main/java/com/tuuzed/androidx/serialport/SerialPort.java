package com.tuuzed.androidx.serialport;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerialPort {

    InputStream getInputStream();

    OutputStream getOutputStream();

    boolean isOpen();

    void shutdown();

}
