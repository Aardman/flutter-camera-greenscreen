package io.flutter.plugins.camera.aardman.fixedfilter;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.PointF;
import android.opengl.GLES20;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;

/**
 * Version of FixedBaseFilter that runs on a fixed pipeline as a thread
 */
public class FixedBaseFilter {

        public static final String NO_FILTER_VERTEX_SHADER = "" +
                "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                " \n" +
                "varying vec2 textureCoordinate;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "    gl_Position = position;\n" +
                "    textureCoordinate = inputTextureCoordinate.xy;\n" +
                "}";
        public static final String NO_FILTER_FRAGMENT_SHADER = "" +
                "varying highp vec2 textureCoordinate;\n" +
                " \n" +
                "uniform sampler2D inputImageTexture;\n" +
                " \n" +
                "void main()\n" +
                "{\n" +
                "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
                "}";

        private final String vertexShader;
        private final String fragmentShader;
        private int glProgId;
        private int glAttribPosition;
        private int glUniformTexture;
        private int glAttribTextureCoordinate;
        private int outputWidth;
        private int outputHeight;
        private boolean isInitialized;

        public FixedBaseFilter() {
            this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        }

        public FixedBaseFilter(final String vertexShader, final String fragmentShader) { 
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }

        private final void init() {
            onInit();
            onInitialized();
        }

        public void onInit() {
            glProgId = OpenGlUtils.loadProgram(vertexShader, fragmentShader);
            glAttribPosition = GLES20.glGetAttribLocation(glProgId, "position");
            glUniformTexture = GLES20.glGetUniformLocation(glProgId, "inputImageTexture");
            glAttribTextureCoordinate = GLES20.glGetAttribLocation(glProgId, "inputTextureCoordinate");
            isInitialized = true;
        }

        public void onInitialized() {
        }

        public void ifNeedInit() {
            if (!isInitialized) init();
        }

        public final void destroy() {
            isInitialized = false;
            GLES20.glDeleteProgram(glProgId);
            onDestroy();
        }

        public void onDestroy() {
        }

        public void onOutputSizeChanged(final int width, final int height) {
            outputWidth = height;
            outputHeight = width;
        }

        public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {
            GLES20.glUseProgram(glProgId);
            if (!isInitialized) {
                return;
            }

            cubeBuffer.position(0);
            GLES20.glVertexAttribPointer(glAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
            GLES20.glEnableVertexAttribArray(glAttribPosition);
            textureBuffer.position(0);
            GLES20.glVertexAttribPointer(glAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                    textureBuffer);
            GLES20.glEnableVertexAttribArray(glAttribTextureCoordinate);
            if (textureId != OpenGlUtils.NO_TEXTURE) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                GLES20.glUniform1i(glUniformTexture, 0);
            }
            onDrawArraysPre();
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(glAttribPosition);
            GLES20.glDisableVertexAttribArray(glAttribTextureCoordinate);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }

        protected void onDrawArraysPre() {
        }

        public boolean isInitialized() {
            return isInitialized;
        }

        public int getOutputWidth() {
            return outputWidth;
        }

        public int getOutputHeight() {
            return outputHeight;
        }

        public int getProgram() {
            return glProgId;
        }

        public int getAttribPosition() {
            return glAttribPosition;
        }

        public int getAttribTextureCoordinate() {
            return glAttribTextureCoordinate;
        }

        public int getUniformTexture() {
            return glUniformTexture;
        }

        protected void setInteger(final int location, final int intValue) {

                    ifNeedInit();
                    GLES20.glUniform1i(location, intValue);

        }

        protected void setFloat(final int location, final float floatValue) {

                    ifNeedInit();
                    GLES20.glUniform1f(location, floatValue);

        }

        protected void setFloatVec2(final int location, final float[] arrayValue) {

                    ifNeedInit();
                    GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));


        }

        protected void setFloatVec3(final int location, final float[] arrayValue) {

                    ifNeedInit();
                    GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));

        }

        protected void setFloatVec4(final int location, final float[] arrayValue) {

                    ifNeedInit();
                    GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));

        }

        protected void setFloatArray(final int location, final float[] arrayValue) {
            ifNeedInit();
            GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
        }

        protected void setPoint(final int location, final PointF point) {
                      ifNeedInit();
                    float[] vec2 = new float[2];
                    vec2[0] = point.x;
                    vec2[1] = point.y;
                    GLES20.glUniform2fv(location, 1, vec2, 0);
           
        }

        protected void setUniformMatrix3f(final int location, final float[] matrix) {
                     ifNeedInit();
                    GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
        
        }

        protected void setUniformMatrix4f(final int location, final float[] matrix) { 
                    ifNeedInit();
                    GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0); 
        }
 

        public static String loadShader(String file, Context context) {
            try {
                AssetManager assetManager = context.getAssets();
                InputStream ims = assetManager.open(file);

                String re = convertStreamToString(ims);
                ims.close();
                return re;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "";
        }

        public static String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
