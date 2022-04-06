package io.flutter.plugins.camera.aardman;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.RequiresApi;

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
 * Hold a reference to the Filter/glProgram pipeline
 * Handle calls for onDraw and onPreviewFrame events for each buffer
 * Schedule rendering
 * Handle some Graphics calculations re: transforms such as rotation and scaling
 *
 *
 * Note that there are two rendering behaviours that ideally would be split out
 * these are a) for the preview rendering and b) for the rendering of the captured
 * still images
 *
 * Due to time pressure these are both in this class currently but are divided into
 * two commented sections with the state separated and commented for future easier
 * splitting
 *
 * GLBridge needs to determine whether what rendering is required so there are two
 * render pathways in the run loop corresponding with that.
 *
*/
public class FilterRenderer implements PreviewFrameHandler,  GLWorker {

    private static final String TAG = "FilterRenderer";

    /**
     * Filters
     */
    private GPUImageFilter glFilter;
    private GPUImageFilter altFilter;

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

    //Indicates that filter is not ready for use
    //boolean filterIsBeingReplaced = false;

    /**
     * Used to provide separate (unshared) objects for capture
     * part of the rendering pipeline
     */
    private FloatBuffer glCaptureTextureBuffer;

    /**
     * OpenGL render control and queue, this acts as the buffer for frames
     * from the camera, access is synchronised, so no locking is needed at draw time
     * from the main openGL render loop
     */
    private Queue<Runnable> openGLTaskQueue;
    //private Queue<Runnable> controlQueue;

    /*********************************************************************************
     *
     *                   Setting up the openGL Filter engine
     *
     *********************************************************************************/

    public FilterRenderer() {
        openGLTaskQueue = new LinkedList<>();
        //controlQueue = new LinkedList<>();
    }

    /**
     *   Setup sequence regarding filters
     *
     *   On initialisation - setup the task queues
     *
     *   OnCreate - setup the GL resources and the identity filter
     *
     *   On enable   - if the identity filter is running and there is no chroma, leave as is
     *
     *   On disable  - if the chroma filter is running swap filters
     *
     *   On update filtering
     *
     *               - reset the current parameters
     *
     *               - if there is no chroma filter, create it with the current parameters
     *                 set it as the relevant active/inactive filter
     *
     *               - if there is a chroma filter, recreate the filters
     *
     *   When recreating the filters
     *               - switch rendering to the identity filter
     *               - destroy and re-create the filter with the current parameters and store
     *                 in the alt filter
     *               - toggle on the chroma filter
     *
     *   Client sequences
     *
     *     - initialise the camera, creates the identity filter and starts the capture
     *     - update filters
     *     - enable/update/disable filters as desired.
     *
     */


    //Setup GL environment
    //Needs to be called from the GL thread
    public void onCreate() {
        setupGLObjects();
        initialiseFilters();
    }

    private void setupGLObjects() {

        glFullScreenQuadBuffer = ByteBuffer.allocateDirect(QUAD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glFullScreenQuadBuffer.put(QUAD).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        glCaptureTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        setRotation(Rotation.NORMAL, false, false);
    }

    private void initialiseFilters() {
        glFilter = new GPUImageFilter();
        glFilter.ifNeedInit();
        glFilter.onOutputSizeChanged(outputWidth, outputHeight);
    }

    private void setFilter(GPUImageChromaKeyBlendFilter filter) {
        altFilter = filter;
        altFilter.ifNeedInit();
        altFilter.onOutputSizeChanged(outputWidth, outputHeight);
    }


    /*********************************************************************************
     *                                Flutter API calls
     *********************************************************************************/


    /**
     * Enable/disable filtering
     */

    public void enableFilter() {
        if (glFilter != null && altFilter != null && !isChroma(glFilter)) {
            toggleFilter();
        }
    }

    public void disableFilter() {
        if (glFilter != null && altFilter != null && isChroma(glFilter)) {
            toggleFilter();
        }
    }

    /**
     * Update parameters
     */

    public void updateParameters(final FilterParameters parameters) {

        //we need to find which filter needs updating
        GPUImageChromaKeyBlendFilter filter = null;
        if (isChroma(altFilter)) {
            filter = (GPUImageChromaKeyBlendFilter) altFilter;
        } else if (isChroma(glFilter)) {
            filter = (GPUImageChromaKeyBlendFilter) glFilter;
        }

        /**
         * If there is no chroma filter, then its first time so we create one
         * the parameters and a background file path
         */
        if (filter == null) {
            appendToTaskQueue( ()->{
                setupChromaFilter(parameters);
            }, openGLTaskQueue);
        }
        else if (filter != null && parameters.backgroundImage != null){
            appendToTaskQueue(()->{
                restartChromaFilter(parameters);
            }, openGLTaskQueue);
        }
        else if (filter != null && filter.getBitmap() == null) {
            CustomFilterFactory.setChromaBackground(filter,
                    new Size(outputWidth, outputHeight),
                    parameters,
                    textureIsLandscape);
        }

        if (parameters.replacementColour != null) {
            float[] colour = parameters.getColorToReplace();
            filter.setColorToReplace(colour[0], colour[1], colour[2]);
        }

        this.previewFilterParameters = parameters;
    }

    /**
     * Helper methods
     */

    /**
     *    - record enablement state
     *    - switch rendering to the identity filter
     *    - destroy and re-create the filter with the current parameters and store
     *      in the alt filter
     *    - restore enablement state
     *
     * @param parameters
     */
    void restartChromaFilter(FilterParameters parameters){

        if(isChroma(glFilter)){
            disableFilter();
        }

        appendToTaskQueue(()->  {
            rebuildChromaFilter(parameters);
            }, openGLTaskQueue );

    }


    //Replaces the chroma filter using the fresh parameters
    void rebuildChromaFilter(FilterParameters parameters){
        if(isChroma(glFilter)){
            return;
        }
        else if(isChroma(altFilter)){
            synchronized (altFilter){
                GPUImageChromaKeyBlendFilter oldFilter = (GPUImageChromaKeyBlendFilter) altFilter;
                oldFilter.destroy();
                oldFilter.notify();
            }
            setupChromaFilter(parameters);
        }
    }


    void setupChromaFilter(FilterParameters parameters){
        GPUImageChromaKeyBlendFilter filter = CustomFilterFactory.getCustomFilter(parameters);
        if (parameters.backgroundImage != null) {
            CustomFilterFactory.setChromaBackground(filter,
                    new Size(outputWidth, outputHeight),
                    parameters,
                    textureIsLandscape);
        }
        setFilter(filter);
    }

    //In general should be set in the disabled 'slot' altFilter storage
    void setNewChromaFilterInAppropriateState(GPUImageChromaKeyBlendFilter filter){
        if(altFilter == null){
            altFilter = filter;
        }
    }

    public void toggleFilter() {
        GPUImageFilter temp = glFilter;
        glFilter = altFilter;
        altFilter = temp;
    }

    public void setIdentityFilterActive(){
        if(glFilter!=null &&  !isChroma(glFilter)){
            return;
        }
        else if(altFilter !=null && !isChroma(altFilter)){
            glFilter = altFilter;
        }
    }

    boolean isChroma(GPUImageFilter filter){
        return filter instanceof GPUImageChromaKeyBlendFilter;
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

        //Copy RGB buffer to glTextureBuffer
        runAll(openGLTaskQueue);

        //Perform the filter operation
        if (glFilter != null) {
            glFilter.onDraw(glTextureId, glFullScreenQuadBuffer, glTextureBuffer);
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
            glCaptureTextureBuffer.clear();
            glCaptureTextureBuffer.put(textureCords).position(0);
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
