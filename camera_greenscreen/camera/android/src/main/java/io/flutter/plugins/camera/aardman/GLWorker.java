package io.flutter.plugins.camera.aardman;

import android.opengl.GLES20;

class GLWorker   {

    //FilterPipelineControllerDelegate delegate;

    public void onCreate() {

    }

    public boolean onDraw() {
        GLES20.glClearColor(0f, 1f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //this.delegate.onDrawCall();
        return true;
    }

    public void onDispose() {
        //
    }

}
