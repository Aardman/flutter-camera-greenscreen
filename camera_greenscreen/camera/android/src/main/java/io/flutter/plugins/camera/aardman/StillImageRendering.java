package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;

public interface StillImageRendering {
    public void  scheduleStillImageFiltering(Bitmap sourceBitmap, Runnable stillImageCompletion);
    public Bitmap getStillImageBitmap();
}
