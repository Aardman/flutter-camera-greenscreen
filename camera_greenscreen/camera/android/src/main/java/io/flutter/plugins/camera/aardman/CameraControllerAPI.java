package io.flutter.plugins.camera.aardman;

import android.hardware.camera2.CameraAccessException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.embedding.engine.systemchannels.PlatformChannel;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.camera.features.Point;
import io.flutter.plugins.camera.features.autofocus.FocusMode;
import io.flutter.plugins.camera.features.exposurelock.ExposureMode;
import io.flutter.plugins.camera.features.flash.FlashMode;

import android.util.Log;

/**
 * Default not implemented parts of the implicit API expected by
 * MethodCallHandledImpl.
 */
public abstract class CameraControllerAPI {

    private static final String LOGTAG = "CameraControllerAPI";

    /**
     * Call API stubs for 'onMethodCall' that must be implemented
     */
    void willImplement(String op){
        Log.i(LOGTAG, "must implement " + op);
    }
    public void open(String imageFormatGroup) throws CameraAccessException { willImplement("open"); }
    public void close() { willImplement("close"); }
    public void takePicture(@NonNull final MethodChannel.Result result){ willImplement("takePicture"); }
    public void startPreview() throws CameraAccessException{  willImplement("startPreview"); }
    public void lockCaptureOrientation(PlatformChannel.DeviceOrientation orientation) {  willImplement("locCapOrientation"); }
    public void unlockCaptureOrientation(){  willImplement("unlocCapOrientation"); }
    public void dispose(){  willImplement("dispose"); }
    public void enableFilters(){  willImplement("enableFilters"); }
    public void disableFilters(){  willImplement("disableFilters"); }
    public void updateFilters(Object arguments){  willImplement("updateFilters"); }

    public void setFlashMode(@NonNull final MethodChannel.Result result, @NonNull FlashMode newMode) {  wontImplement(); }

    /**
     * Call API stubs not required by Aardman animator camera
     */
    void wontImplement(){
        Log.i(LOGTAG, "won't implement");
    }
    public void startVideoRecording(@NonNull MethodChannel.Result result) {  wontImplement(); }
    public void stopVideoRecording(@NonNull MethodChannel.Result result) {  wontImplement(); }
    public void pauseVideoRecording(@NonNull MethodChannel.Result result) {  wontImplement(); }
    public void resumeVideoRecording(@NonNull MethodChannel.Result result) {  wontImplement(); }
    public void setExposureMode(@NonNull final MethodChannel.Result result, @NonNull ExposureMode newMode){  wontImplement(); }
    public void setExposurePoint(@NonNull final MethodChannel.Result result, @Nullable Point point) {  wontImplement(); }
    public double getMinExposureOffset() { wontImplement();     return 0;}
    public double getMaxExposureOffset() { wontImplement();     return 0;}
    public double getExposureOffsetStepSize() { wontImplement();return 0;}
    public void setExposureOffset(@NonNull final MethodChannel.Result result, double offset) { wontImplement(); }
    public void setFocusMode(final MethodChannel.Result result, @NonNull FocusMode newMode) { wontImplement(); }
    public void setFocusPoint(@NonNull final MethodChannel.Result result, @Nullable Point point){ wontImplement(); }
    public void startPreviewWithImageStream(EventChannel imageStreamChannel) { wontImplement(); }
    public float getMaxZoomLevel() { wontImplement(); return 0;}
    public float getMinZoomLevel() { wontImplement(); return 0;}
    public void setZoomLevel(@NonNull final MethodChannel.Result result, float zoom) throws CameraAccessException { wontImplement(); }
    public void pausePreview() {  wontImplement(); }
    public void resumePreview() {  wontImplement(); }
}
