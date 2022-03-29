package io.flutter.plugins.camera.aardman;

import android.graphics.SurfaceTexture;
import android.opengl.GLDebugHelper;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.Writer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.GLTextureView;

public class GLBridge implements Runnable {
    private static final String LOG_TAG = "EglBridge.GLWorker";
    protected final SurfaceTexture flutterTexture;
    private EGL10 egl;
    private EGLDisplay eglDisplay;
    private EGLContext eglContext;
    private EGLSurface eglSurface;

    private GL10 gl = null;

    private boolean running;

    private  GLWorker worker;

    public GLBridge(SurfaceTexture flutterTexture,  GLWorker worker) {
        this.flutterTexture = flutterTexture;
        this.running = true;
        this.worker = worker;

        Thread thread = new Thread(this);
        thread.setName("GLThread");
        //Starts the GLThread in this class
        thread.start();
    }

    private void initGL() {
        egl = (EGL10) EGLContext.getEGL();
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        int[] version = new int[2];
        if (!egl.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }

        EGLConfig eglConfig = chooseEglConfig();
        eglContext = createContext(egl, eglDisplay, eglConfig);

        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, flutterTexture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            throw new RuntimeException("GL Error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }

        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("GL make current error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }

        /** Get GL for rendering */
        gl = (GL10) eglContext.getGL();
    }


    private void deinitGL() {
        egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(eglDisplay, eglSurface);
        egl.eglDestroyContext(eglDisplay, eglContext);
        egl.eglTerminate(eglDisplay);
        Log.d(LOG_TAG, "OpenGL deinit OK.");
    }

    //As per guarded run in GPUImageFilter guarded run
    @Override
    public void run() {
        initGL();
        worker.onCreate();
        Log.d(LOG_TAG, "OpenGL init OK.");

            while (running) {
                //Draw operations are to the current eglSurface
//                synchronized(worker) {
//                    if (worker.isAwaitingRender()) {
                 worker.onDrawFrame();  //if there is something to do, do it.... queued on GLThread
//                    }
//                }
                //Swap from current eglSurface to display surface
                if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                    Log.d(LOG_TAG, String.valueOf(egl.eglGetError()));
                }
            }


        worker.onDispose();
        deinitGL();
    }

    private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        int[] attribList = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attribList);
    }

    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = getConfig();

        if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("Failed to choose config: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        } else if (configsCount[0] > 0) {
            return configs[0];
        }

        return null;
    }

    private int[] getConfig() {
        return new int[]{
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_SAMPLE_BUFFERS, 1,
                EGL10.EGL_SAMPLES, 4,
                EGL10.EGL_NONE
        };
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        running = false;
    }

    public void onDispose() {
        running = false;
    }


}