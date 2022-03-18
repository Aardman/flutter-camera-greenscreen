package io.flutter.plugins.camera.aardman;

import android.os.Handler;
import android.os.HandlerThread;

import io.flutter.plugins.camera.CameraProperties;
import io.flutter.plugins.camera.DartMessenger;
import io.flutter.plugins.camera.features.resolution.ResolutionPreset;
import io.flutter.view.TextureRegistry;

/**
 * A camera controller that uses a filter pipeline for rendering its Flutter texture
 *
 * Note this is called 'FilterCameraCONTROLLER' because it is not a device camera object
 * but a controller for a camera
 *
 * Initial scope, setup and run a camera preview
 */
public class CameraController extends CameraControllerAPI {

    private static final String  TAG = "FilterCameraController";

    /**
     * Input dependencies - inject all required dependencies in constructor
     */
    private final TextureRegistry.SurfaceTextureEntry flutterTexture;
    private final DartMessenger dartMessenger;
    private final CameraProperties cameraProperties;
    private final ResolutionPreset resolutionPreset;

    /**
     * Background tasks
     */
    /** A {@link Handler} for running tasks in the background. */
    private Handler backgroundHandler;
    /** An additional thread for running tasks that shouldn't block the UI. */
    private HandlerThread backgroundHandlerThread;

    public CameraController(
            final TextureRegistry.SurfaceTextureEntry flutterTexture,
            final DartMessenger dartMessenger,
            final CameraProperties cameraProperties,
            final ResolutionPreset resolutionPreset ) {

        this.flutterTexture = flutterTexture;
        this.dartMessenger = dartMessenger;
        this.cameraProperties = cameraProperties;
        this.resolutionPreset = resolutionPreset;
    }

    /**
     * Friday target
     *
     * Add GPU rendering as per sample code
     * via a FilterGPUCamera
     */


}
