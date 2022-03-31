package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/** Saves a Bitmap {@link Bitmap} into the specified {@link File}. */
public class BitmapSaver implements Runnable {

    /**
     * The Bitmap to convert to JPEG and save to a file
     */
    protected final Bitmap bitmap;

    /**
     * The file we save the image into.
     */
    protected final File file;

    /**
     * Used to report the status of the save action.
     */
    protected final io.flutter.plugins.camera.ImageSaver.Callback callback;

    /**
     * Creates an instance of the ImageSaver runnable
     *
     * @param bitmap   - The bitmap to save
     * @param file     - The file to save the image to
     * @param callback - The callback that is run on completion, or when an error is encountered.
     */
    public BitmapSaver(@NonNull Bitmap bitmap, @NonNull File file, @NonNull io.flutter.plugins.camera.ImageSaver.Callback callback) {
        this.bitmap = bitmap;
        this.file = file;
        this.callback = callback;
    }

    @Override
    public void run() {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] jpegBytes = stream.toByteArray();

        FileOutputStream output = null;

        try {

            if (jpegBytes != null) {
                output = new FileOutputStream(file);
                output.write(jpegBytes);
            }
            callback.onComplete(file.getAbsolutePath());

        } catch (IOException e) {
            callback.onError("IOError", "Failed saving image");
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    callback.onError("cameraAccess", e.getMessage());
                }
            }
        }
    }
}



