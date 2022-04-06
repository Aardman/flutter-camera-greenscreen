package io.flutter.plugins.camera.aardman;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.util.HashMap;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.util.Rotation;

/**
 * Acts as the controller of the capture flow
 * for the filtered version of the application
 *
 * Part of the purpose of this class is to shim the new capture flow off from the
 * main Flutter Camera Plugin, this adopts the main responsibilities that are
 * to be altered.
 *
 * Responsibilities
 *
 * Setup and tear down the pipeline
 * Update in flight parameters
 *
 */
public class FilterPipelineController {

     private static final String TAG = "FilterPipeController";

    /**
     * The camera captures to a surface managed by this imageReader
     */
    ImageReader filterImageReader;

    /**
     * The eglBridge manages the OpenGL pipeline
     */
    GLBridge eglBridge;

    /**
     * The current openGL rendering thread
     * <p>
     * The renderer that takes imageReader captured frames and applies the filtering
     * <p>
     * That stage could be the GPUImageFilters, or native as in Chornenko example
     */
    FilterRenderer filterRenderer;

    /**
     * Current window dimensions
     */
    Size viewSize;

    /**
     * From DeviceOrientationManager.getPhotoOrientation
     *
     *  {0:LANDSCAPE_RIGHT, 180:LANDSCAPE_LEFT, 90:PORTRAIT_UP, 270:PORTRAIT_DOWN}
     *
     *  These are all relevant for the rear camera. Landscape is flipped for the front camera.
     *
     */
    int deviceOrientation = 0;

    /**
     *  GPU_IMAGE has a Rotation enum that corresponds to the following mapping
     *
     *  {0:NORMAL, 180:ROTATION_180, 90:ROTATION_90, 270:ROTATION_270}
     *
     *  This is updated synchronously wit deviceOrientation
     */
    Rotation gpuImageRotation;

    public boolean isLandscape(){
        return (deviceOrientation == 0 || deviceOrientation == 180);
    }

    /**
     * For handling still image capture
     */
    FilterParameters currentFilterParameters = new FilterParameters();
    Context context = null;
    Bitmap currentBitmap = null;
    Boolean filtersEnabled = false;

    /*********************
     *  Initialisation   *
     *********************/

    public FilterPipelineController(SurfaceTexture flutterTexture, Activity activity) {
        //The filter or filter group
        //eglBridge will start openGL session running
        //init filter on the glThread
        this.context = activity.getApplicationContext();
        filterRenderer = new FilterRenderer();
        filterRenderer.previewFilterParameters = currentFilterParameters;
        GLWorker glWorker = (GLWorker) filterRenderer;
        this.eglBridge = new GLBridge(flutterTexture, glWorker);
    }

    /**
     * Must be set before rendering commences
     *
     * @param previewSize
     */
    public void setSize(Size previewSize) {
        this.viewSize = previewSize;
        ((GLWorker) filterRenderer).setSize(previewSize);
    }

    /**
     * This is set on initialisation and before capturing an image. With the Animator app
     * this only needs to be set on initialisation as the camera is re-created whenever the
     * device changes orientation. However it is also set when initiating a
     * still capture even though its not needed for the Animator use case
     * @param orientation
     */
    public void setOrientation(int orientation){
        deviceOrientation = orientation;
        gpuImageRotation = Rotation.fromInt(orientation);
        if (filterRenderer != null){
            //does not have correct effect, rotates the entire preview unnecessarily
            //probably the need is just to rotate the inpput bitmap to the filter itself
           //filterRenderer.setRotation(gpuImageRotation);
        }
    }

    /**
     *  Used by the Camera controller to setup the CaptureSession
     *  this imageReader provides the target surface for capturing
     *  the camera input
     */
     public Surface getImageReaderSurface() {

         this.filterImageReader =
                 ImageReader.newInstance(
                         viewSize.getWidth(),
                         viewSize.getHeight(),
                         //This old fashioned format is expected by GPUImage native library for RGB conversion YUV_420_888
                         //and its the one used for a camera preview
                         ImageFormat.YUV_420_888,
                         2);

         GLWorker glWorker =  (GLWorker) this.filterRenderer;

         PreviewOnImageAvailableListener previewOnImageAvailableListener = new PreviewOnImageAvailableListener((PreviewFrameHandler) glWorker);

         /**
          *  Note this ImageReader takes a null handler ref as it will run on the calling thread
          *  which is the camera preview callback thread, so there is no need to set handler explicitly
          */
         this.filterImageReader.setOnImageAvailableListener(previewOnImageAvailableListener, null);

         return this.filterImageReader.getSurface();
    }

    /**********************************
     *      Still Image Handling      *
     **********************************/
     public void filterStillImage(Bitmap stillImageBitmap, Runnable stillImageCompletion){
         updateCurrentBitmap(stillImageBitmap);
         stillImageCompletion.run();
     }

     void updateCurrentBitmap(Bitmap stillImageBitmap) {
         if(!filtersEnabled){
             this.currentBitmap = stillImageBitmap;
         }
         else {
             if (this.context == null) {
                 Log.e(TAG, "No Application Context available for GPUImage rendering");
             }
             GPUImage gpuImage = new GPUImage(this.context);
             Size outputSize = new Size(stillImageBitmap.getWidth(), stillImageBitmap.getHeight());
             GPUImageChromaKeyBlendFilter filter = CustomFilterFactory.getCustomFilter(this.currentFilterParameters);
             CustomFilterFactory.setChromaBackground(filter, outputSize, currentFilterParameters);
             gpuImage.setFilter(filter);
             this.currentBitmap = gpuImage.getBitmapWithFilterApplied(stillImageBitmap);
         }
     }

    public Bitmap getLastFilteredResult(){
         return this.currentBitmap;
    }

    /*********************
     *      Updates      *
     *********************/

    public void disableFilter(){
        this.filtersEnabled = false;
        filterRenderer.disableFilter();
    }

    public void enableFilter(){
        this.filtersEnabled = true;
        filterRenderer.enableFilter();
    }

    public void updateParameters(FilterParameters parameters){
        this.currentFilterParameters = parameters;
        filterRenderer.updateParameters(parameters);
    }


}
