package com.tuuzed.androidx.serialport;

import com.tuuzed.androidx.serialport.annotation.DataBits;
import com.tuuzed.androidx.serialport.annotation.Parity;
import com.tuuzed.androidx.serialport.annotation.StopBits;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;

public class SerialPortBuilder {
    private String device;
    private int baudRate = 9600;
    private int dataBits = DataBits.DATA_BIT_8;
    private int stopBits = StopBits.STOP_BIT_1;
    private int parity = Parity.NONE;

    public SerialPortBuilder(@NonNull String device) {
        this.device = device;
    }

    public SerialPortBuilder setDevice(@NonNull String device) {
        this.device = device;
        return this;
    }

    public SerialPortBuilder setBaudRate(int baudRate) {
        this.baudRate = baudRate;
        return this;
    }

    public SerialPortBuilder setDataBits(@DataBits int dataBits) {
        this.dataBits = dataBits;
        return this;
    }

    public SerialPortBuilder setStopBits(@StopBits int stopBits) {
        this.stopBits = stopBits;
        return this;
    }

    public SerialPortBuilder setParity(@Parity int parity) {
        this.parity = parity;
        return this;
    }

    @NonNull
    public SerialPort build() throws IOException, SecurityException {
        return new NativeSerialPort(new File(device), baudRate, dataBits, stopBits, parity);
    }


    @NonNull
    public AsyncSerialPort buildAsyncSerialPort(int bufferCapacity) throws IOException, SecurityException {
        return new AsyncSerialPort(new File(device), baudRate, dataBits, stopBits, parity, bufferCapacity);
    }

    @NonNull
    public AsyncSerialPort buildAsyncSerialPort(AsyncSerialPort.Listener listener) throws IOException, SecurityException {
        return new AsyncSerialPort(new File(device), baudRate, dataBits, stopBits, parity)
                .setListener(listener);
    }

    @NonNull
    public AsyncSerialPort buildAsyncSerialPort(int bufferCapacity, AsyncSerialPort.Listener listener) throws IOException, SecurityException {
        return new AsyncSerialPort(new File(device), baudRate, dataBits, stopBits, parity, bufferCapacity)
                .setListener(listener);
    }

}
