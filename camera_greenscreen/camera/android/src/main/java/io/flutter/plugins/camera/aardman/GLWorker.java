package io.flutter.plugins.camera.aardman;

import android.opengl.GLES20;

interface GLWorker {
    public boolean isAwaitingRender();
    public void setAwaitingRender(boolean awaitingRender);
    public void onCreate();
    public void onDispose();
    public void onDrawFrame();
}
