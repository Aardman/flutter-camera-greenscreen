package io.flutter.plugins.camera.aardman;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "ImageAvailableListener";

    PreviewFrameHandler output;

        public ImageAvailableListener(PreviewFrameHandler previewFrameHandler){
            this.output = previewFrameHandler;
        }

        @Override
        public void onImageAvailable(ImageReader reader) {

            Image image = reader.acquireNextImage();

            //This operation is fast
            byte [] data = generateNV21Data(image);

            int width = image.getWidth();
            int height = image.getHeight();

            image.close();

            output.onPreviewFrame( data, width, height);
        }


    public static final byte[] generateNV21Data(@NotNull Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        Image.Plane var10000 = planes[0];
        //Intrinsics.checkNotNullExpressionValue(planes[0], "planes[0]");
        byte[] rowData = new byte[var10000.getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        int i = 0;
        // Intrinsics.checkNotNullExpressionValue(planes, "planes");

        for(int var11 = planes.length; i < var11; ++i) {
            switch(i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
            }

            var10000 = planes[i];
            // Intrinsics.checkNotNullExpressionValue(planes[i], "planes[i]");
            ByteBuffer buffer = var10000.getBuffer();
            var10000 = planes[i];
            // Intrinsics.checkNotNullExpressionValue(planes[i], "planes[i]");
            int rowStride = var10000.getRowStride();
            var10000 = planes[i];
            // Intrinsics.checkNotNullExpressionValue(planes[i], "planes[i]");
            int pixelStride = var10000.getPixelStride();
            int shift = i == 0 ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            int row = 0;

            for(int var19 = h; row < var19; ++row) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, w);
                    channelOffset += w;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    int col = 0;

                    for(int var22 = w; col < var22; ++col) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
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



