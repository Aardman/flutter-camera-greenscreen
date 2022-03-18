package io.flutter.plugins.camera;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.camera.aardman.CameraController;
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

    private @Nullable ImageReader filterTargetImageReader;

    public CameraPipelineLoader(TextureRegistry textureRegistry, BinaryMessenger messenger, Activity activity){
        this.textureRegistry = textureRegistry;
        this.messenger = messenger;
        this.activity = activity;
    }

    //Setup a filtered pipeline and register the required texture
    //with Flutter.
    private TextureRegistry.SurfaceTextureEntry makePipeline(){
        TextureRegistry.SurfaceTextureEntry surfaceTextureEntry =
                textureRegistry.createSurfaceTexture();
        return surfaceTextureEntry;
    }

    /*
     *  ImageReader will be the source of the registered texture.
     *  Let's start by adding an additional one.
     *
     *  Uncertain where this is to be performed.
     */
    private void registerNewTextureEntry(SurfaceTexture surfaceTexture){
        textureRegistry.registerSurfaceTexture(surfaceTexture);
    }

    private void createTargetSurfaceTexture(){
        filterTargetImageReader = ImageReader.newInstance(640, 480, ImageFormat.YV12, 30);
        Surface surface = filterTargetImageReader.getSurface();
//        {
//            mImageReader.setOnImageAvailableListener(mImageAvailListener, mCameraHandler);
//        }
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

    public CameraController instantiateFilterCameraPipeline(MethodCall call, MethodChannel.Result result) throws CameraAccessException {
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

        CameraController camera =
                new CameraController(
                        flutterSurfaceTexture,
                        dartMessenger,
                        cameraProperties,
                        resolutionPreset
                );

        Map<String, Object> reply = new HashMap<>();
        reply.put("cameraId", flutterSurfaceTexture.id());
        result.success(reply);

        return camera;
    }

}





//    ImageReader targetImageReader = (ImageReader) pictureImageReader;
//    Surface picSurface = targetImageReader.getSurface();
//    SurfaceTexture picTexture = picSurface.surfaceTexture;
//
//    previewRequestBuilder.addTarget(targetImageReader.getSurface());

//    ImageReader.OnImageAvailableListener mImageAvailListener = new ImageReader.OnImageAvailableListener() {
//        @Override
//        public void onImageAvailable(ImageReader reader) {
//            //when a buffer is available from the camera
//            //get the image
//            Image image = reader.acquireNextImage();
//
//            Log.i(TAG, "Image Available" + image);

//        Image.Plane[] planes = image.getPlanes();
//
//        //copy it into a byte[]
//        byte[] outFrame = new byte[mFrameSize];
//        int outFrameNextIndex = 0;
//
//
//        ByteBuffer sourceBuffer = planes[0].getBuffer();
//        sourceBuffer.get(tempYbuffer, 0, tempYbuffer.length);
//
//        ByteBuffer vByteBuf = planes[1].getBuffer();
//        vByteBuf.get(tempVbuffer);
//
//        ByteBuffer yByteBuf = planes[2].getBuffer();
//        yByteBuf.get(tempUbuffer);

            //free the Image
//            image.close();
//        }
//    };

//targetImageReader.setOnImageAvailableListener(mImageAvailListener, backgroundHandler);

