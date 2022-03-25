package io.flutter.plugins.camera.aardman;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import io.flutter.plugins.camera.aardman.fixedfilter.*;

/**
*
 * Responsibilities
 *
 * Manage the double buffered rendering process
 * Hold a reference to the Filter/glProgram pipeline
 * Handle calls for onDraw and onPreviewFrame events for each buffer
 * Schedule rendering on GLWorker
 *
 * TODO: Is this needed? How much is done already by the client code (Plugin)
 * Handle Graphics calculations re: transforms such as rotation and scaling
 *
*/
public class FilterRenderer implements PreviewFrameHandler,  GLWorker {

    private static final String TAG = "FilterRenderer";

    /**
     * Dependencies
     */
    private FilterParameters filterParameters;
    //We do not require, as all openGL operations are on the current eglSurface
    //private SurfaceTexture filterTexture;
    private FixedBaseFilter glFilterProgramWrapper;

    /**
     * Display parameters
     */
    private int outputWidth = 720 ;
    private int outputHeight = 480 ;
    private int imageWidth = 720 ;
    private int imageHeight = 480;

    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;

    /**
     * OpenGL parameters
     */
    private static final int NO_IMAGE = -1;
    public static final float QUAD[] = {
            -1.f, 1.f,
            -1.f, -1.f,
            1.f, 1.f,
            1.f, -1.f
    };

    private int glTextureId = NO_IMAGE;
    private FloatBuffer glFullScreenQuadBuffer;
    private FloatBuffer glTextureBuffer;
    private IntBuffer glRgbBuffer;

    /**
     * OpenGL render state
     */
    public boolean awaitingRenderOperation = false;

    /*********************************************************************************
     *
     *                   Setting up the openGL Filter engine
     *
     *********************************************************************************/

    public FilterRenderer(FixedBaseFilter filter) {
        setFilter(filter);
        setupGLParameters();
    }

    private void setupGLParameters(){
        glFullScreenQuadBuffer = ByteBuffer.allocateDirect(QUAD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glFullScreenQuadBuffer.put(QUAD).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    private void setFilter(FixedBaseFilter filter){
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


    /*********************************************************************************
     *
     *                   PreviewFrameHandler (Camera Thread)
     *
     *********************************************************************************/

     /**
     * GPUImage provides the GPUImageNativeLibrary with a native
     * implementation for converting NV21 (YUV) planar byte array to RGB
     * which is needed to load the input texture corresponding to glTextureId
     */
    public void onPreviewFrame(final byte[] data, final int width, final int height) {

        if (glRgbBuffer == null) {
            glRgbBuffer = IntBuffer.allocate(width * height);
        }

        //Is this handled on the same thread as for GPUImage?
        GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbBuffer.array());
        glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

        if (imageWidth != width) {
            imageWidth = width;
            imageHeight = height;
            adjustImageScaling();
        }

        //Request Render operation on the filter on the GL rendering thread
        requestRender();

        //worker will then schedule the call to onDraw to render the filter and
        //trigger the buffer swap
    }



    /*********************************************************************************
     *
     *          Filter Draw Calls - including GLWorker Implementation
     *
     *********************************************************************************/

    public void requestRender() {
        awaitingRenderOperation = true;
    }

    public boolean isAwaitingRender() {
        return awaitingRenderOperation;
    }

    public void setAwaitingRender(boolean awaitingRender){
        awaitingRenderOperation = awaitingRender;
    }

    public void onCreate() {}
    public void onDispose() {}

    private double _tick = 0;

    public void onDrawFrame() {
        _tick = _tick + Math.PI / 60;
        float green = (float) ((Math.sin(_tick) + 1) / 2);
        GLES20.glClearColor(0f, green, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);


       //Just write that glTextureId to the current eglSurface

       //glFilterProgramWrapper.onDraw(glTextureId, glFullScreenQuadBuffer, glTextureBuffer);

       awaitingRenderOperation = false;
    }


    /*********************************************************************************
     *
     *                      Still Image Capture Helper
     *
     *********************************************************************************/

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


    /*********************************************************************************
     *
     *                          Graphics helper functions
     *
     *********************************************************************************/

    protected int getFrameWidth() {
        return outputWidth;
    }

    protected int getFrameHeight() {
        return outputHeight;
    }

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

            float[] cube = QUAD;
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
                        QUAD[0] / ratioHeight, QUAD[1] / ratioWidth,
                        QUAD[2] / ratioHeight, QUAD[3] / ratioWidth,
                        QUAD[4] / ratioHeight, QUAD[5] / ratioWidth,
                        QUAD[6] / ratioHeight, QUAD[7] / ratioWidth,
                };
            }

            glFullScreenQuadBuffer.clear();
            glFullScreenQuadBuffer.put(cube).position(0);
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
