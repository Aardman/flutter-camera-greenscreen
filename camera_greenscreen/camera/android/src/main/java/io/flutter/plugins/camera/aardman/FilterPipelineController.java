package io.flutter.plugins.camera.aardman;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import java.util.HashMap;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

/**
 * Acts as the controller of the capture flow
 * for the filtered version of the application
 *
 * Part of the purpose of this class is to shim the new capture flow off from the
 * main Flutter Camera Plugin, this adopts the main responsibilities that are
 * to be altered.
 */
public class FilterPipelineController {

    /** The camera captures to a surface managed by this imageReader */
    ImageReader filterImageReader;

    /** The eglBridge manages the OpenGL pipeline */
    GLBridge eglBridge;

    /** The current openGL rendering thread */
    GLWorker glWorker;

    /** The renderer that takes imageReader captured frames and applies the filters */
    FilterRenderer filterRenderer;

    /** Current window dimensions */
    Size viewSize;

    /*********************
     *  Initialisation   *
     *********************/

    public FilterPipelineController(SurfaceTexture flutterTexture) {

        //eglBridge will start openGL session running
        GLWorker glWorker = new GLWorker();
        this.eglBridge = new GLBridge(flutterTexture, glWorker);

        //The filter or filter group
        GPUImageFilter filter = new GPUImageChromaKeyBlendFilter();

        //renderer
        this.filterRenderer = new FilterRenderer(filter);
        this.filterRenderer.initialiseParameters(new FilterParameters());
    }

    /**
     * Must be set before rendering commences
     * @param windowSize
     */
    public void setSize(Size windowSize) {
        this.viewSize = windowSize;
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
                         ImageFormat.YUV_420_888,
                         2);

         ImageAvailableListener imageAvailableListener = new ImageAvailableListener(filterRenderer);
         this.filterImageReader.setOnImageAvailableListener(imageAvailableListener, null);

         return this.filterImageReader.getSurface();
    }


    /*********************************
     *      Rendering Callback       *
     ********************************/

     void onDraw(){
         //Schedule draw call on openGL GLWorker
     }

    /*********************
     *      Updates      *
     *********************/

    public void updateParameters(FilterParameters parameters){
        this.filterRenderer.updateFilterParameters(parameters);
    }

    /*********************
     *      Disposal     *
     *********************/

    //Do we need to pass back the capture callback?
//    public SurfaceTexture setUpCapturePipeline() {
//
//        //initaliseImageReader(captureListener);
//
//        Surface filterSurface = filterImageReader.getSurface();
//        SurfaceTexture filterTexture = null; // = (SurfaceTexture) filterSurface;
//        return filterTexture;
//    }

    void initialiseImageReader() {
        //filterImageReader = new ImageReader.OnImageAvailableListener();
    }


}