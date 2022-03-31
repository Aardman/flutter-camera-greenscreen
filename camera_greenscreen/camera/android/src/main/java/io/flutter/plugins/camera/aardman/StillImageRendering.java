package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;

/**
 * Created to clarify the role of the renderer to the clients on the camera capture thread
 */
public interface StillImageRendering {
    public void onCaptureFrame(Bitmap sourceBitmap, Runnable stillImageCompletion);
    public Bitmap getFilteredCaptureFrame();
}
