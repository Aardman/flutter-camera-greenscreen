package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;
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
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;

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

    //Handling the bitmaps
    private EGLSurface eglPixelBufferSurface;
    private Bitmap bitmap;
    private int captureWidth;
    private int captureHeight;
    private int format; //GL format
    private int pixelBufferSize;



    public GLBridge(SurfaceTexture flutterTexture,  GLWorker worker) {
        this.flutterTexture = flutterTexture;
        this.running = true;
        this.worker = worker;

        Thread thread = new Thread(this);
        thread.setName("GLThread");
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

        //This is purely for rendering out captures
        eglPixelBufferSurface = egl.eglCreatePbufferSurface(eglDisplay, eglConfig,null);

        makeFlutterOutputCurrent();

        /** Get GL for rendering */
        gl = (GL10) eglContext.getGL();
    }

    private void makeFlutterOutputCurrent(){
        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("GL make flutterOutputSurface current error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }
    }

    private void makeStillImagePixelBufferCurrent(){
        if (!egl.eglMakeCurrent(eglDisplay, eglPixelBufferSurface, eglPixelBufferSurface, eglContext)) {
            throw new RuntimeException("GL make eglPixelBufferSurface current error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }
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
                //if performing the filtering, render to the pixel buffer surface
                if (worker.isFilteringStillImage()){
                    makeStillImagePixelBufferCurrent();
                    //Read pixels to buffer that can be used to pull bitmaps
//                    gl.glReadPixels(0,0, captureWidth, captureHeight, format, pixelBufferSize, eglPixelBufferSurface);
//                    createBitmapFromPixelBuffer();
                    worker.filterStillImage(null); //will just read the current buffer data into the output bitmap
                    makeFlutterOutputCurrent();
                }
                else {
                    worker.onDrawFrame();
                    //Swap from current eglSurface to display surface
                    if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                        Log.d(LOG_TAG, String.valueOf(egl.eglGetError()));
                    }
               }
            }
        worker.onDispose();
        deinitGL();
    }

    //This is copied from the GPUImage Pixelbuffer class, but it is unclear how this
    //works.
    //It could be that Bitmap.createBitmap, as a side effect, uses the current openGL surface
    //as the source of the data that  is used. (ie: its initialised from the current surface data)
    //rather than zeroed memory.
    //If so, this is undocumented by
    public void createBitmapFromPixelBuffer(SurfaceTexture eglPixelBufferSurface, int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        GPUImageNativeLibrary.adjustBitmap(bitmap);
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