package io.flutter.plugins.camera.aardman;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.nio.ByteBuffer;

public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "ImageAvailableListener";

    FilterImageInput output;

        public ImageAvailableListener(FilterImageInput filterImageInput){
            this.output = filterImageInput;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {

            Image image = reader.acquireNextImage();

            byte [] data = YUV_420_888_data(image);

            int width = image.getWidth();
            int height = image.getHeight();

            image.close();

            //size of data actualy
            output.onPreviewFrame( data, width, height);
        }


    private static byte[] YUV_420_888_data(Image image) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[imageWidth * imageHeight *
                ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        int offset = 0;

        for (int plane = 0; plane < planes.length; ++plane) {
            final ByteBuffer buffer = planes[plane].getBuffer();
            final int rowStride = planes[plane].getRowStride();
            // Experimentally, U and V planes have |pixelStride| = 2, which
            // essentially means they are packed.
            final int pixelStride = planes[plane].getPixelStride();
            final int planeWidth = (plane == 0) ? imageWidth : imageWidth / 2;
            final int planeHeight = (plane == 0) ? imageHeight : imageHeight / 2;
            if (pixelStride == 1 && rowStride == planeWidth) {
                // Copy whole plane from buffer into |data| at once.
                buffer.get(data, offset, planeWidth * planeHeight);
                offset += planeWidth * planeHeight;
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride.
                byte[] rowData = new byte[rowStride];
                for (int row = 0; row < planeHeight - 1; ++row) {
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < planeWidth; ++col) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Last row is special in some devices and may not contain the full
                // |rowStride| bytes of data.
                // See http://developer.android.com/reference/android/media/Image.Plane.html#getBuffer()
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                for (int col = 0; col < planeWidth; ++col) {
                    data[offset++] = rowData[col * pixelStride];
                }
            }
        }

        return data;
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
//       byte[] outFrame = new byte[mFrameSize];
//         int outFrameNextIndex = 0;
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



