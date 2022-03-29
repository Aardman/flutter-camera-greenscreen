package io.flutter.plugins.camera.aardman;

import android.opengl.GLES20;
import android.util.Size;

interface GLWorker {
    public void setSize(Size size);
//    public boolean isAwaitingRender();
//    public void setAwaitingRender(boolean awaitingRender);
    public void onCreate();
    public void onDispose();
    public void onDrawFrame();
}
