package io.flutter.plugins.camera.aardman;

import android.opengl.GLES20;
import android.util.Log;
import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

/**
*
 * Responsibilities
 *
 * Manage the double buffered rendering process
 * Hold a reference to the Filters and which of them is enabled on each draw cycle
 * Handle calls for onDraw and onPreviewFrame events for the GLThread and CameraThread respectively
 * Schedule rendering
 * Handle some Graphics calculations
 *
*/
public class FilterRenderer implements PreviewFrameHandler,  GLWorker {

    private static final String TAG = "FilterRenderer";

    /**
     * Filters
     */
    private GPUImageChromaKeyBlendFilter glFilter;
    private GPUImageFilter copyFilter;  //shows preview with no effect, copy input pixels to output
    private boolean glFilterIsEnabled = false;
    private boolean needsToRestoreEnabledFilterAfterRebuild = false;

    /**
     * Display parameters
     */
    private int outputWidth;
    private int outputHeight;
    private int imageWidth;
    private int imageHeight;

    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;

    /**
     * OpenGL parameters
     */
    private static final int NO_IMAGE = -1;

    //Vector for a quad (rectangle) to cover the whole screen
    public static final float QUAD[] = {
            -1.f, 1.f,
            -1.f, -1.f,
            1.f, 1.f,
            1.f, -1.f
    };

    private int glTextureId = NO_IMAGE;

    //Will be populated from QUAD
    private FloatBuffer glFullScreenQuadBuffer;

    private FloatBuffer glTextureBuffer;

    //On each frame will be used to buffer from the preview data
    private IntBuffer glRgbPreviewBuffer;

    //texture rotation used to rotate bitmap when it is added to the
    //chroma filter
    private boolean textureIsLandscape = true;

    /**
     * Filter parameters
     */
    FilterParameters previewFilterParameters;


    /**
     * OpenGL render control and queue, this acts as the buffer for frames
     * from the camera, access is synchronised, so no locking is needed at draw time
     * from the main openGL render loop
     */
    private Queue<Runnable> openGLTaskQueue;

    /*********************************************************************************
     *
     *                   Setting up the openGL Filter engine
     *
     *********************************************************************************/

    public FilterRenderer() {
        openGLTaskQueue = new LinkedList<>();
    }

    /**
     *   Setup sequence regarding filters
     *
     *   On initialisation - setup the task queue and identity filter
     *
     *   OnCreate    - setup the GL resources and the identity filter
     *
     *   On enable   - glFilter will be used to render
     *   On disable  - copyFilter will be used to render
     *
     *   On update filtering
     *
     *               - reset the current parameters that do not require recreating the filter
     *
     *               - if there is no chroma filter, create it with the parameters supplied
     *                 in the update (on the GLThread)
     *
     *               - if there is a chroma filter and it has no bitmap assigned
     *                    assign a bitmap (on the GLThread)
     *
     *               - if there is a chroma filter
     *                     - if enabled, record was enabled state
     *                       (on the GLThread)
     *                     - destroy and re-create the filter with the current parameters and store
     *                     - if was enabled, enable the filter
     *
     *   Client sequences
     *
     *     - initialise the camera  (creates the identity filter and starts the capture)
     *     - enabling filter has no effect until updateFilters has been called the first time to set the background
     *     - update filters
     *     - enable disable filter at will
     *
     */

    //Setup GL environment
    //Needs to be called from the GL thread
    public void onCreate() {
        setupGLObjects();
        initialiseCopyFilter();
    }

    void initialiseCopyFilter(){
        copyFilter = new GPUImageFilter();
        copyFilter.ifNeedInit();
        copyFilter.onOutputSizeChanged(outputWidth, outputHeight);
    }

    private void setupGLObjects() {

        glFullScreenQuadBuffer = ByteBuffer.allocateDirect(QUAD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glFullScreenQuadBuffer.put(QUAD).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        setRotation(Rotation.NORMAL, false, false);
    }

    private void setGLFilter(GPUImageChromaKeyBlendFilter filter) {
        glFilter = filter;
        glFilter.ifNeedInit();
        glFilter.onOutputSizeChanged(outputWidth, outputHeight);
    }


    /*********************************************************************************
     *                                Flutter API calls
     *********************************************************************************/

    /**
     * Enable/disable filtering
     */

    public void enableFilter() {
        if (glFilter != null)
          glFilterIsEnabled = true;
    }

    public void disableFilter() {
        glFilterIsEnabled = false;
    }

    /**
     * Update parameters
     */

    public void updateParameters(final FilterParameters parameters) {

        previewFilterParameters.updateWith(parameters);

        //Set simple parameters if there is a glFilter available
        if (glFilter != null && previewFilterParameters.replacementColour != null) {
            float[] colour = previewFilterParameters.getColorToReplace();
            glFilter.setColorToReplace(colour[0], colour[1], colour[2]);
        }

        //Set simple parameters if there is a glFilter available
        if (glFilter != null && previewFilterParameters.getSensitivity()!= Constants.FLOAT_NOT_SET) {
            glFilter.setThresholdSensitivity(previewFilterParameters.getSensitivity());
        }

        /**
         * Setting the main filter background
         */
        if (glFilter == null) {
            appendToTaskQueue( ()->{
                setupChromaFilter(previewFilterParameters);
            }, openGLTaskQueue);
        }
        //filter needs to be initially set if parameters were set without a background before
        else if (glFilter != null && glFilter.getBitmap() == null) {
            CustomFilterFactory.setChromaBackground(glFilter,
                    new Size(outputWidth, outputHeight),
                    previewFilterParameters,
                    textureIsLandscape);
        }
        //filter needs replacing
        else if (glFilter != null && previewFilterParameters.backgroundImage != null){
            appendToTaskQueue(()->{
                restartChromaFilter(previewFilterParameters);
            }, openGLTaskQueue);
        }

    }

    /**
     * Helper methods
     */

    /**
     *
     *   Should be called on GLThread
     *
     *    - record enablement state
     *    - switch rendering to the identity filter
     *    - destroy and re-create the filter with the current parameters and store
     *      in the alt filter
     *    - restore enablement state
     *
     * @param parameters
     */
    void restartChromaFilter(FilterParameters parameters){

        synchronized (glFilter){
            if (glFilterIsEnabled) {
                 needsToRestoreEnabledFilterAfterRebuild = true;
                 disableFilter();
             }
            glFilter.destroy();
            glFilter.notify();
        }

        setupChromaFilter(parameters);

        //restore enabled if it was set
        if(needsToRestoreEnabledFilterAfterRebuild){
            needsToRestoreEnabledFilterAfterRebuild = false;
            enableFilter();
        }

    }

    void setupChromaFilter(FilterParameters parameters){
        GPUImageChromaKeyBlendFilter filter = CustomFilterFactory.getCustomFilter(parameters);
        /**
         * Will add a coloured background if none is supplied as an indication of error condition
         */
        CustomFilterFactory.setChromaBackground(filter,
                new Size(outputWidth, outputHeight),
                parameters,
                textureIsLandscape);

        setGLFilter(filter);
    }

    public void setTextureIsLandscape(boolean isLandscape) {
        textureIsLandscape = isLandscape;
    }

    public GPUImageFilter getFilter() {
        return glFilter;
    }

    /*********************************************************************************
     *
     *                        Preview Filtered Rendering
     *
     *********************************************************************************/

    /*********************************************************************************
     *                   PreviewFrameHandler (Camera Thread)
     *********************************************************************************/

     /**
     * GPUImage provides the GPUImageNativeLibrary with a native
     * implementation for converting NV21 (YUV) planar byte array to RGB
     * which is needed to load the input texture corresponding to glTextureId
     */
    public void onPreviewFrame(final byte[] data, final int width, final int height) {

        if (glRgbPreviewBuffer == null) {
            glRgbPreviewBuffer = IntBuffer.allocate(width * height);
        }
        if (openGLTaskQueue.isEmpty()) {
            appendToTaskQueue(() -> {
                GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbPreviewBuffer.array());
                glTextureId = OpenGlUtils.loadTexture(glRgbPreviewBuffer, width, height, glTextureId);

                if (imageWidth != width) {
                    imageWidth = width;
                    imageHeight = height;
                    adjustImageScalingAndInitialiseBuffers();
                }

            }, openGLTaskQueue);
        }
        else {
            Log.i(TAG, "DROPPED A FRAME FROM THE PREVIEW INPUT");
        }
 
    }

    /*********************************************************************************
     *          GLThread - handling tasks to run on the GLThread
     *********************************************************************************/

        protected void appendToTaskQueue(final Runnable runnable, Queue<Runnable> queue) {
            synchronized (queue) {
                openGLTaskQueue.add(runnable);
            }
        }

        private void runAll(Queue<Runnable> queue) {
            synchronized (queue) {
                while (!queue.isEmpty()) {
                    queue.poll().run();
                }
            }
        }

    /*********************************************************************************
     *             GLWorker Implementation - The main Filter Draw Calls
     *********************************************************************************/

    public void setSize(Size size){
        this.outputHeight = size.getHeight();
        this.outputWidth  = size.getWidth();
    }


    //Called by GLBridge (GLThread)
    public void onDrawFrame() {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //copy the RGB buffer to glTextureBuffer ready for filtering (only 1 task per call is expected)
        //or recreate, update or create the glFilter if this is happening
        runAll(openGLTaskQueue);

        GPUImageFilter filter = glFilterIsEnabled ? glFilter : copyFilter;

        if (filter != null) {
            filter.onDraw(glTextureId, glFullScreenQuadBuffer, glTextureBuffer);
        }

    }

    public void onDispose() {}

    /*********************************************************************************
     *
     *                          Graphics helper functions
     *
     *                             Copied from GPUImage
     *
     *********************************************************************************/

    private void adjustImageScalingAndInitialiseBuffers() {
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

        private float addDistance(float coordinate, float distance) {
            return coordinate == 0.0f ? distance : 1 - distance;
        }

        public void setRotation(final Rotation rotation) {
            this.rotation = rotation;
            adjustImageScalingAndInitialiseBuffers();
        }

        public void setRotation(final Rotation rotation,
                                final boolean flipHorizontal, final boolean flipVertical) {
            this.flipHorizontal = flipHorizontal;
            this.flipVertical = flipVertical;
            setRotation(rotation);
        }


}
