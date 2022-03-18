package io.flutter.plugins.camera;

import android.app.Activity;
import android.hardware.camera2.CameraAccessException;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.camera.features.CameraFeatureFactoryImpl;
import io.flutter.plugins.camera.features.resolution.ResolutionPreset;
import io.flutter.view.TextureRegistry;

/**
 * Flutter requires that the target texture for capture sessions is instantiated
 * on the main thread. Therefore we need to setup the target texture at the time we
 * instantiate the camera.
 *
 * The name represents two responsibilities
 *
 * - firstly creating a 'pipeline',  ie: establishing
 * the target texture used to render previews and as the source for an ImageReader for the
 * filter pipeline. This texture is the 'endpoint' of the preview capture pipeline. As such
 * any filtering application must create this texture on the main thread and establish it
 * as the texture passed back to flutter when the camera is instantiated.
 *
 * - secondly instantiating the Camera2 camera object and starting its background thread
 * responsible to rendering to the pipeline
 *
 */
public class CameraPipelineLoader {

    private final TextureRegistry textureRegistry;
    private final BinaryMessenger messenger;
    private final Activity activity;

    public CameraPipelineLoader(TextureRegistry textureRegistry, BinaryMessenger messenger, Activity activity){
        this.textureRegistry = textureRegistry;
        this.messenger = messenger;
        this.activity = activity;
    }

    //Make and register a texture with the TextureRegistry and all
    //the inbetween bits
    private TextureRegistry.SurfaceTextureEntry makePipeline(){
        TextureRegistry.SurfaceTextureEntry surfaceTextureEntry =
                textureRegistry.createSurfaceTexture();
        return surfaceTextureEntry;
    }

    public Camera instantiateCameraPipeline(MethodCall call, MethodChannel.Result result) throws CameraAccessException {
        String cameraName = call.argument("cameraName");
        String preset = call.argument("resolutionPreset");
        boolean enableAudio = call.argument("enableAudio");

        TextureRegistry.SurfaceTextureEntry flutterSurfaceTexture = makePipeline();

        DartMessenger dartMessenger =
                new DartMessenger(
                        messenger, flutterSurfaceTexture.id(), new Handler(Looper.getMainLooper()));
        CameraProperties cameraProperties =
                new CameraPropertiesImpl(cameraName, CameraUtils.getCameraManager(activity));
        ResolutionPreset resolutionPreset = ResolutionPreset.valueOf(preset);

        Camera camera =
                new Camera(
                        activity,
                        flutterSurfaceTexture,
                        new CameraFeatureFactoryImpl(),
                        dartMessenger,
                        cameraProperties,
                        resolutionPreset,
                        enableAudio);

        Map<String, Object> reply = new HashMap<>();
        reply.put("cameraId", flutterSurfaceTexture.id());
        result.success(reply);

        return camera;
    }

}
