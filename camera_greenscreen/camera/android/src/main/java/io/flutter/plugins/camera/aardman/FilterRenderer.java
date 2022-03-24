package io.flutter.plugins.camera.aardman;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

/**
*
 * Responsibilities
 *
 * Manage the rendering process
 * Hold a reference to the Filter/glProgram pipeline (if using non GPUImage filter)
 * Handle calls for onDraw and onPreviewFrame events for each buffer
 * Schedule buffer transfer to the Flutter surface texture
 *
 * TODO: Is this needed? How much is done already by the client code (Plugin)
 * Handle Graphics calculations re: transforms such as rotation and scaling
 *
*/
public class FilterRenderer implements FilterImageInput {

    private static final String TAG = "FilterRenderer";

    /** Dependencies */
    private SurfaceTexture hardwareTexture;
    private GPUImageFilter glFilterProgramWrapper;
    private FilterParameters filterParameters;

    /** Display parameters */
    private int outputWidth;
    private int outputHeight;
    private int imageWidth;
    private int imageHeight;

    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;


    /** OpenGL parameters */
    private static final int NO_IMAGE = -1;
    public static final float CUBE[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };
    private int glTextureId = NO_IMAGE;
    private SurfaceTexture surfaceTexture = null;
    private FloatBuffer glCubeBuffer;
    private FloatBuffer glTextureBuffer;
    private IntBuffer glRgbBuffer;


    public FilterRenderer(GPUImageFilter filter) {
        setFilter(filter);
        setupGLParameters();
    }

    private void setupGLParameters(){
        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    public void initialiseSurfaceTextureForFlutterDisplay(SurfaceTexture flutterTexture){
        hardwareTexture = flutterTexture;
    }

    private void setFilter(GPUImageFilter filter){
        glFilterProgramWrapper = filter;
        glFilterProgramWrapper.ifNeedInit();
        GLES20.glUseProgram(filter.getProgram());
        glFilterProgramWrapper.onOutputSizeChanged(outputWidth, outputHeight);
    }

    void updateFilterParameters(FilterParameters filterParameters){
          ///update input to the filter
          this.filterParameters = filterParameters;
      }

      void initialiseParameters(FilterParameters filterParameters){
          this.filterParameters = filterParameters;
      }

    protected int getFrameWidth() {
        return outputWidth;
    }

    protected int getFrameHeight() {
        return outputHeight;
    }


    /**
     *  FilterImageInput Implementation
     */
    public void handleNextImageFrame(){
        Log.i(TAG, "Image Frame Received on FilterImageInput");
        //
    }

    /**
     * Render still frame as a bitmap
     */

    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, true);
    }

    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        Bitmap resizedBitmap = null;
        if (bitmap.getWidth() % 2 == 1) {
            resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1, bitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            resizedBitmap.setDensity(bitmap.getDensity());
            Canvas can = new Canvas(resizedBitmap);
            can.drawARGB(0x00, 0x00, 0x00, 0x00);
            can.drawBitmap(bitmap, 0, 0, null);
        }

        glTextureId = OpenGlUtils.loadTexture(
                resizedBitmap != null ? resizedBitmap : bitmap, glTextureId, recycle);
        if (resizedBitmap != null) {
            resizedBitmap.recycle();
        }
        imageWidth = bitmap.getWidth();
        imageHeight = bitmap.getHeight();
        adjustImageScaling();
    }

    /**
     * Graphics helper functions
     */

        private void adjustImageScaling() {
            float outputWidth = this.outputWidth;
            float outputHeight = this.outputHeight;
            if (rotation == Rotation.ROTATION_270 || rotation == Rotation.ROTATION_90) {
                outputWidth = this.outputHeight;
                outputHeight = this.outputWidth;
            }

            float ratio1 = outputWidth / imageWidth;
            float ratio2 = outputHeight / imageHeight;
            float ratioMax = Math.max(ratio1, ratio2);
            int imageWidthNew = Math.round(imageWidth * ratioMax);
            int imageHeightNew = Math.round(imageHeight * ratioMax);

            float ratioWidth = imageWidthNew / outputWidth;
            float ratioHeight = imageHeightNew / outputHeight;

            float[] cube = CUBE;
            float[] textureCords = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
            if (scaleType == GPUImage.ScaleType.CENTER_CROP) {
                float distHorizontal = (1 - 1 / ratioWidth) / 2;
                float distVertical = (1 - 1 / ratioHeight) / 2;
                textureCords = new float[]{
                        addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                        addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                        addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                        addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
                };
            } else {
                cube = new float[]{
                        CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                        CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                        CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                        CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
                };
            }

            glCubeBuffer.clear();
            glCubeBuffer.put(cube).position(0);
            glTextureBuffer.clear();
            glTextureBuffer.put(textureCords).position(0);
        }

        public void setScaleType(GPUImage.ScaleType scaleType) {
            this.scaleType = scaleType;
        }

        private float addDistance(float coordinate, float distance) {
            return coordinate == 0.0f ? distance : 1 - distance;
        }

        public void setRotationCamera(final Rotation rotation, final boolean flipHorizontal,
                                      final boolean flipVertical) {
            setRotation(rotation, flipVertical, flipHorizontal);
        }

        public void setRotation(final Rotation rotation) {
            this.rotation = rotation;
            adjustImageScaling();
        }

        public void setRotation(final Rotation rotation,
                                final boolean flipHorizontal, final boolean flipVertical) {
            this.flipHorizontal = flipHorizontal;
            this.flipVertical = flipVertical;
            setRotation(rotation);
        }

        public Rotation getRotation() {
            return rotation;
        }

        public boolean isFlippedHorizontally() {
            return flipHorizontal;
        }

        public boolean isFlippedVertically() {
            return flipVertical;
        }


    }
