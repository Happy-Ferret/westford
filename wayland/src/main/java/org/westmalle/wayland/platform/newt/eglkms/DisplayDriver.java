package org.westmalle.wayland.platform.newt.eglkms;


import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.egl.EGL;
import com.sun.jna.Pointer;

import org.westmalle.wayland.platform.CLibrary;
import org.westmalle.wayland.platform.newt.eglkms.gbm.GbmLibrary;

import jogamp.newt.DisplayImpl;
import jogamp.opengl.egl.EGLDisplayUtil;

public class DisplayDriver extends DisplayImpl {

    private int fd;
    private Pointer gbmDevice;

    @Override
    protected void createNativeImpl() {
        this.fd = CLibrary.INSTANCE.open("/dev/dri/card0",
                                         CLibrary.O_RDWR);
        this.gbmDevice = GbmLibrary.INSTANCE.gbm_create_device(this.fd);

        final EGLGraphicsDevice eglGraphicsDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(
                Pointer.nativeValue(gbmDevice),
                AbstractGraphicsDevice.DEFAULT_CONNECTION,
                AbstractGraphicsDevice.DEFAULT_UNIT);
        eglGraphicsDevice.open();

        final String extensions = EGL.eglQueryString(eglGraphicsDevice.getHandle(),
                                                     EGL.EGL_EXTENSIONS);

        if (!extensions.contains("EGL_KHR_surfaceless_opengl")) {
            System.err.println("no surfaceless support, cannot initialize\n");
            System.exit(1);
        }

        this.aDevice = eglGraphicsDevice;
    }

    @Override
    protected void closeNativeImpl(final AbstractGraphicsDevice aDevice) {
        aDevice.close();
        GbmLibrary.INSTANCE.gbm_device_destroy(this.gbmDevice);
        this.gbmDevice = null;
        CLibrary.INSTANCE.close(this.fd);
        this.fd = 0;
    }

    @Override
    protected void dispatchMessagesNative() {
        //TODO
    }

    public int getFd() {
        return this.fd;
    }

    public Pointer getGbmDevice() {
        return this.gbmDevice;
    }
}
