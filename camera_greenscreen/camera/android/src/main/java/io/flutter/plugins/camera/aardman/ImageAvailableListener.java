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
            //Image img = reader.acquireNextImage();
            output.handleNextImageFrame();
        }

}



//    ImageReader targetImageReader = (ImageReader) pictureImageReader;
//    Surface picSurface = targetImageReader.getSurface();
//    SurfaceTexture picTexture = picSurface.surfaceTexture;
//
//    previewRequestBuilder.addTarget(targetImageReader.getSurface());

//    ImageReader.OnImageAvailableListener mImageAvailListener = new ImageReader.OnImageAvailableListener() {
//        @Override
//        public void onImageAvailable(ImageReader reader) {
//            //when a buffer is available from the camera
//            //get the image
//            Image image = reader.acquireNextImage();
//
//            Log.i(TAG, "Image Available" + image);

//        Image.Plane[] planes = image.getPlanes();
//
//        //copy it into a byte[]
//        byte[] outFrame = new byte[mFrameSize];
//        int outFrameNextIndex = 0;
//
//
//        ByteBuffer sourceBuffer = planes[0].getBuffer();
//        sourceBuffer.get(tempYbuffer, 0, tempYbuffer.length);
//
//        ByteBuffer vByteBuf = planes[1].getBuffer();
//        vByteBuf.get(tempVbuffer);
//
//        ByteBuffer yByteBuf = planes[2].getBuffer();
//        yByteBuf.get(tempUbuffer);

//free the Image
//            image.close();
//        }
//    };

//targetImageReader.setOnImageAvailableListener(mImageAvailListener, backgroundHandler);


