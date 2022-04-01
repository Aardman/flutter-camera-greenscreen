package io.flutter.plugins.camera.aardman;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.util.HashMap;

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

    /*********************
     *  Initialisation   *
     *********************/

    public FilterPipelineController(SurfaceTexture flutterTexture) {
        //The filter or filter group
        //eglBridge will start openGL session running
        //init filter on the glThread
        filterRenderer = new FilterRenderer();
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
     * Must be set before still capture commences
     *
     * @param captureSize
     */
    public void setStillCaptureSize(Size captureSize) {
       this.eglBridge.setupStillCapture(captureSize);
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
         filterRenderer.onCaptureFrame(stillImageBitmap, stillImageCompletion);
     }

    public Bitmap getLastFilteredResult(){
         return filterRenderer.getFilteredCaptureFrame();
    }

    /*********************
     *      Updates      *
     *********************/

    public void updateParameters(FilterParameters parameters){
        filterRenderer.updateFilterParameters(parameters);
    }

    public void disableFilter(){
        filterRenderer.disableFilter();
    }

    public void enableFilter(){
        filterRenderer.enableFilter();
    }


    /*********************
     *      Disposal     *
     *********************/


}
