package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Size;

interface GLWorker  {
    public void setSize(Size size);
    public void onCreate();
    public void onDispose();
    public void onDrawFrame();
}
