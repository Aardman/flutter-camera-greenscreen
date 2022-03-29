package io.flutter.plugins.camera.aardman;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import android.Manifest;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.Size;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.File;
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
import jp.co.cyberagent.android.gpuimage.filter.GPUImageToonFilter;
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
*/
public class FilterRenderer implements PreviewFrameHandler,  GLWorker {

    private static final String TAG = "FilterRenderer";

    /**
     * Dependencies
     */
    private FilterParameters filterParameters;
    //We do not require, as all openGL operations are on the current eglSurface
    //private SurfaceTexture filterTexture;
    private GPUImageFilter glFilter;

    /**
     * Display parameters
     */
    private int outputWidth ;
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

    //On each frame will be populated by loading from glRgbBuffer
    private FloatBuffer glTextureBuffer;

    //On each frame will be populated from the preview data
    private IntBuffer glRgbBuffer;

    /**
     * OpenGL render control and queue
     */
    private Queue<Runnable> openGLTaskQueue;

    /*********************************************************************************
     *
     *                   Setting up the openGL Filter engine
     *
     *********************************************************************************/

    @RequiresApi(api = Build.VERSION_CODES.R)
    public FilterRenderer( ) {
        openGLTaskQueue = new LinkedList<>();
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

    private void setFilter(GPUImageFilter filter){
        glFilter = filter;
        glFilter.ifNeedInit();
        glFilter.onOutputSizeChanged(outputWidth, outputHeight);
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
        if (openGLTaskQueue.isEmpty()) {
            appendToTaskQueue(new Runnable() {
                @Override
                public void run() {
                    GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbBuffer.array());
                    glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                    if (imageWidth != width) {
                        imageWidth = width;
                        imageHeight = height;
                        adjustImageScaling();
                    }

                }
            });
        }
        else {
            Log.i(TAG, "DROPPED A FRAME FROM THE PREVIEW INPUT");
        }
 
    }

    /*********************************************************************************
     *
     *          GLThread - handling tasks to run on the GLThread
     *
     *********************************************************************************/

        protected void appendToTaskQueue(final Runnable runnable) {
            synchronized (openGLTaskQueue) {
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
     *
     *             GLWorker Implementation - The main Filter Draw Calls
     *
     *********************************************************************************/

    public void setSize(Size size){
        this.outputHeight = size.getHeight();
        this.outputWidth  = size.getWidth();
    }

    //Setup GL environment
    //Needs to be called from the GLThread
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void onCreate() {
        setupGLParameters();
       // setFilter(new GPUImageToonFilter());
        createChromaKeyFilter();
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
     *                       Create the ChromaKey filter
     *
     *********************************************************************************/

      //Get a sample bitmap for the background  (Jurassic)

      //Create a new instance of the class
    @RequiresApi(api = Build.VERSION_CODES.R)
    void createChromaKeyFilter() {
           GPUImageChromaKeyBlendFilter chromaFilter =   new GPUImageChromaKeyBlendFilter();
           Bitmap redBitmap = createImage(720, 480, Color.RED);
//         File bitmapFile = new File(Environment.getExternalStorageDirectory() + "/" + "0000-0001/Documents/demo_720.jpg");
//         Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
           chromaFilter.setBitmap(redBitmap);
           setFilter(chromaFilter);
      }

    /**
     * Generates a solid colour
     * @param width
     * @param height
     * @param color
     * @return A one color image with the given width and height.
     */
    public static Bitmap createImage(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);
        return bitmap;
    }

    /*********************************************************************************
     *
     *                           Bitmap helper
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
     *                             Copied from GPUImage
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
