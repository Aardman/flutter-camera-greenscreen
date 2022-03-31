package io.flutter.plugins.camera.aardman;

import io.flutter.plugins.camera.ImageSaver;

import androidx.annotation.NonNull;
import java.io.File;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;

public class StillImageFilterProcessor implements Runnable {

    /** The JPEG input image  */
    protected final Image image;

    /** The file we save the filtered image into. */
    protected final File file;

    protected final Handler backgroundHandler;

    /** Used to report the status of the eventual file saving action. */
    protected final ImageSaver.Callback callback;

    FilterPipelineController filterPipeline;

    public StillImageFilterProcessor(@NonNull Image image,
                               @NonNull File file,
                               @NonNull Handler backgroundHandler,
                               @NonNull ImageSaver.Callback callback,
                               @NonNull FilterPipelineController filterPipeline) {
        this.image = image;
        this.file = file;
        this.backgroundHandler = backgroundHandler;
        this.callback = callback;
        this.filterPipeline = filterPipeline;
    }

    public void run() {
        //extract JPEG data
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        //recycle the image reader
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        image.close();

        //Create a bitmap that can be input to the filtering process
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, buffer.limit());

        //Run a filter operation on the filter pipeline using the captured image
        filterPipeline.filterStillImage(bitmap, () -> {
             //Runs on GLThread, filteredBitmap will be available.
             Bitmap filteredBitmap = filterPipeline.getLastFilteredResult();

             backgroundHandler.post(
                  new BitmapSaver (
                      filteredBitmap,
                      file,
                      callback
                   )
             );
        });
    }
}

