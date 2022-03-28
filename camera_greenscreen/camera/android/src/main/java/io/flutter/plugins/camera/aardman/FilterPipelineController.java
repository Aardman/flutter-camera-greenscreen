package io.flutter.plugins.camera.aardman;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import io.flutter.plugins.camera.aardman.fixedfilter.FixedBaseFilter;
import io.flutter.plugins.camera.aardman.fixedfilter.FixedGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

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

    /** The camera captures to a surface managed by this imageReader */
    ImageReader filterImageReader;

    /** The eglBridge manages the OpenGL pipeline */
    GLBridge eglBridge;

    /**
     *
     * The current openGL rendering thread
     *
     * The renderer that takes imageReader captured frames and applies the filtering
     *
     * That stage could be the GPUImageFilters, or native as in Chornenko example
     *
     * */
    GLWorker glWorker;

    /** Current window dimensions */
    Size viewSize;

    /*********************
     *  Initialisation   *
     *********************/

    public FilterPipelineController(SurfaceTexture flutterTexture) {

        //The filter or filter group
        //eglBridge will start openGL session running
        //init filter on the glThread
        FilterRenderer renderer = new FilterRenderer();
        glWorker =  (GLWorker) renderer;
        this.eglBridge = new GLBridge(flutterTexture, glWorker);

        //TODO:
        // filterRenderer.initialiseParameters(new FilterParameters());
    }

    /**
     * Must be set before rendering commences
     * @param windowSize
     */
    public void setSize(Size previewSize) {
        this.viewSize = previewSize;
        glWorker.setSize(previewSize);
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
                         ImageFormat.YUV_420_888,
                         2);

         ImageAvailableListener imageAvailableListener = new ImageAvailableListener((PreviewFrameHandler) glWorker);

         /**
          *  Note this ImageReader takes a null handler ref as it will run on the calling thread
          *  which is the camera preview callback thread, so there is no need to set handler explicitly
          */
         this.filterImageReader.setOnImageAvailableListener(imageAvailableListener, null);

         return this.filterImageReader.getSurface();
    }


    /*********************
     *      Updates      *
     *********************/

    public void updateParameters(FilterParameters parameters){
        //TODO:
        //this.filterRenderer.updateFilterParameters(parameters);
    }

    /*********************
     *      Disposal     *
     *********************/


}
