package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Size;

import androidx.annotation.Nullable;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter;


//Used to create the custom filter for both preview and capture variations
public class CustomFilterFactory {

    /**
     * Filter
     */
    public static GPUImageChromaKeyBlendFilter getCustomFilter(FilterParameters parameters) {
        GPUImageChromaKeyBlendFilter chromaFilter = new GPUImageChromaKeyBlendFilter();

        float[] colour = parameters.getColorToReplace();
        if (colour == null) {
            colour = Constants.DEFAULT_COLOUR; //defaults to green if not set
        }
        chromaFilter.setColorToReplace(colour[0], colour[1], colour[2]);
        return chromaFilter;
    }

    public static void setChromaBackground(GPUImageChromaKeyBlendFilter filter, Size outputSize, FilterParameters parameters, boolean isLandscape) {
        //gets a sized and prepared background image to match the size of the captured image or preview
        Bitmap captureBackground = CustomFilterFactory.getBackground(parameters.backgroundImage, outputSize, isLandscape);
        filter.setBitmap(captureBackground);
    }

    /**
     * Gets the background scaled and cropped to the desired targetSize
     *
     * @param filePath    fully qualified path the the background image source
     * @param targetSize  desired size of the background
     * @param isLandscape
     * @return
     */
    public static Bitmap getBackground(@Nullable String filePath, Size targetSize, boolean isLandscape) {
        Bitmap backgroundBitmap = BitmapFactory.decodeFile(filePath);
        if (backgroundBitmap == null) {
            //Use solid magenta background to indicate an error condition if loading the background file is unsuccesful
            backgroundBitmap = CustomFilterFactory.createImage(targetSize.getWidth(), targetSize.getHeight(), Color.MAGENTA);
        }
        return prepareBitmap(backgroundBitmap, targetSize, isLandscape);
    }

    /**
     * Prepare the bitmap for use in the chroma filter
     * <p>
     * Note that backgrounds are assumed to be widescreen landscape images
     * these need to be scaled, cropped and translated to fit the height of the preview
     * or capture image that they are being composited with.
     * <p>
     * The widescreen image scaling ratio is determined by the height, which is always scaled
     * to fit the height of the capture or preview image
     * <p>
     * Then this resized image is translated in the X dimension so that its center is
     * centered relative to the preview or capture image
     * <p>
     * Then the image is effectively cropped to the output size in a Bitmap.createBitmap
     * operation that uses the matrix combining identity x scale x translate operations
     *
     * @param sourceBitmap The source bitmap/background image
     * @param targetSize   The desired output size of the image
     * @param isLandscape Indicates the orientation to be processed
     * @return a scaled and translated bitmap that is a center crop of the input, for the portrait
     * orientation the resulting bitmap is rotated ready for applying as the filter texture.
     */

    public static Bitmap prepareBitmap(Bitmap sourceBitmap, Size targetSize, boolean isLandscape) {

        int w = sourceBitmap.getWidth();
        int h = sourceBitmap.getHeight();

        int outputWidth  = targetSize.getWidth();
        int outputHeight = targetSize.getHeight();

        if(!isLandscape) {
            outputWidth  = targetSize.getHeight();
            outputHeight = targetSize.getWidth();
        }

        //calculate scale from height
        float scale_factor = ((float) h / (float) outputHeight);
        float scaledWidth = w / scale_factor;

        //add scale transformation
        Matrix matrix = new Matrix();
        matrix.postScale(1 / scale_factor, 1 / scale_factor);

        //Create the output bitmap with the supplied transforms
        Bitmap outputBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, w, h, matrix, true);

        //Now need to crop and translate
        int translationInX = (int) (scaledWidth - outputWidth) / 2;
        outputBitmap = Bitmap.createBitmap(outputBitmap, translationInX, 0, outputWidth, outputHeight);

        if(!isLandscape){
            outputBitmap = rotateBitmap(outputBitmap);
        }

        sourceBitmap.recycle();

        return outputBitmap;
    }

    static Bitmap rotateBitmap(Bitmap sourceBitmap) {
        Matrix rotationMatrix = new Matrix();
        float rotation = -90.f;
        rotationMatrix.postRotate(rotation);
        return Bitmap.createBitmap(sourceBitmap,
                0,
                0,
                sourceBitmap.getWidth(),
                sourceBitmap.getHeight(),
                rotationMatrix,
                true);
    }

    /**
     * Generates a solid colour
     * @param width
     * @param height
     * @param color
     * @return A one color image with the given width and height.
     */
    public static Bitmap createImage(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);
        return bitmap;
    }

}
