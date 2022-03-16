package io.flutter.plugins.camera;

import android.graphics.SurfaceTexture;
import android.media.ImageReader;
import android.view.Surface;

import java.io.FileReader;
import java.util.HashMap;

/**
 * Acts as the controller of the capture flow
 * for the filtered version of the application
 *
 * Part of the purpose of this class is to shim the new capture flow off from the
 * main Flutter Camera Plugin, this adopts the main responsibilities that are
 * to be altered.
 */
public class FilterController {

    FilterParameters filterParameters;
    ImageReader filterImageReader;

    public FilterController() {
        this.filterParameters = new FilterParameters();
    }

    public void updateParameters(HashMap parameters){
        this.filterParameters.update(parameters);
        System.out.println("Hue changed to value:" + this.filterParameters.chromaKeyRange.getHue());
    }

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
