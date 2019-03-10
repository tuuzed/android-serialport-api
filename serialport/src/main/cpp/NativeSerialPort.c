#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>
#include <strings.h>

#include "NativeSerialPort.h"

#include "android/log.h"

static const char *TAG = "serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static void throw_exception(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    }
    /* free the local ref */
    (*env)->DeleteLocalRef(env, cls);
}

static speed_t getBaudRate(jint baudRate) {
    switch (baudRate) {
        case 0:
            return B0;
        case 50:
            return B50;
        case 75:
            return B75;
        case 110:
            return B110;
        case 134:
            return B134;
        case 150:
            return B150;
        case 200:
            return B200;
        case 300:
            return B300;
        case 600:
            return B600;
        case 1200:
            return B1200;
        case 1800:
            return B1800;
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 500000:
            return B500000;
        case 576000:
            return B576000;
        case 921600:
            return B921600;
        case 1000000:
            return B1000000;
        case 1152000:
            return B1152000;
        case 1500000:
            return B1500000;
        case 2000000:
            return B2000000;
        case 2500000:
            return B2500000;
        case 3000000:
            return B3000000;
        case 3500000:
            return B3500000;
        case 4000000:
            return B4000000;
        default:
            return -1;
    }
}

/*
 * Class:     com_tuuzed_androidx_serialport_NativeSerialPort
 * Method:    nativeOpen
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL
Java_com_tuuzed_androidx_serialport_NativeSerialPort_nativeOpen(
        JNIEnv *env, jclass type, jstring path,
        jint baudRate, jint dataBit, jint stopBit,
        jint parity
) {
    int fd;
    speed_t speed;
    jobject mFileDescriptor;

    /* Opening device */
    {
        jint flags = 0;
        jboolean iscopy;
        const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
        LOGD("Opening serial port %s with flags 0x%x", path_utf, O_RDWR | flags);
        fd = open(path_utf, O_RDWR | O_NONBLOCK);
        LOGD("open() fd = %d", fd);
        (*env)->ReleaseStringUTFChars(env, path, path_utf);
        if (fd == -1) {
            /* Throw an exception */
            LOGE("Cannot open port");
            throw_exception(env, "java/lang/IOException", "Cannot open port");
            return NULL;
        }
    }

    /* Configure device */
    {
        speed = getBaudRate(baudRate);
        if (speed == -1) {
            throw_exception(env, "java/lang/IOException", "Invalid baudRate");
            LOGE("Invalid baudRate");
            return NULL;
        }
        struct termios cfg;
        LOGD("Configuring serial port");
        if (tcgetattr(fd, &cfg)) {
            LOGE("tcgetattr() failed");
            close(fd);
            throw_exception(env, "java/lang/IOException", "tcgetattr() failed");
            return NULL;
        }

        cfmakeraw(&cfg);
        //设置波特率
        cfsetispeed(&cfg, speed);
        cfsetospeed(&cfg, speed);
        switch (parity) {
            case 0:
                break;
            case 1:
                cfg.c_cflag |= PARENB;
                break;
            case 2:
                cfg.c_cflag &= ~PARODD;
                break;
            default:
                throw_exception(env, "java/lang/IllegalArgumentException", "Invalid parity");
                break;
        }
        switch (dataBit) {
            case 5:
                cfg.c_cflag |= CS5;
                break;
            case 6:
                cfg.c_cflag |= CS6;
                break;
            case 7:
                cfg.c_cflag |= CS7;
                break;
            case 8:
                cfg.c_cflag |= CS8;
                break;
            default:
                throw_exception(env, "java/lang/IllegalArgumentException", "Invalid dataBit");
                break;
        }
        switch (stopBit) {
            case 1:
                cfg.c_cflag &= ~CSTOPB;
                break;
            case 2:
                cfg.c_cflag |= CSTOPB;
                break;
            default:
                throw_exception(env, "java/lang/IllegalArgumentException", "Invalid stopBit");
                break;
        }
        // 不管能否读取到数据，read都立即返回
        cfg.c_cc[VTIME] = 0;//设置等待时间
        cfg.c_cc[VMIN] = 0;//设置最小接收字符

        tcflush(fd, TCIFLUSH);
        if (tcsetattr(fd, TCSANOW, &cfg)) {
            LOGE("tcsetattr() failed");
            close(fd);
            throw_exception(env, "java/lang/IOException", "tcsetattr() failed");
            return NULL;
        }
    }

    /* Create a corresponding file descriptor */
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        (*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint) fd);
    }

    return mFileDescriptor;

}

/*
 * Class:     com_tuuzed_androidx_serialport_NativeSerialPort
 * Method:    nativeClose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_tuuzed_androidx_serialport_NativeSerialPort_nativeClose(
        JNIEnv *env, jobject instance
) {
    jclass SerialPortClass = (*env)->GetObjectClass(env, instance);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, instance, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}