package io.flutter.plugins.camera.aardman;

import android.media.ImageReader;
import android.util.Log;

public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "ImageAvailableListener";

    FilterImageInput output;

        public ImageAvailableListener(FilterImageInput filterImageInput){
            this.output = filterImageInput;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "onImageAvailable");
            // reader.acquireNextImage();
            output.handleNextImageFrame();
        }

}
