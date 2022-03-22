/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * PF note
 * It was necessary to 'fork' this as it was not possible to customise it via subclassing
 * due to private members and unwanted interface implementations.
 */

package io.flutter.plugins.camera.aardman;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageNativeLibrary;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

public class GPURenderer  {

        private static final int NO_IMAGE = -1;
        public static final float CUBE[] = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,
        };

        protected GPUImageFilter filter;

        public final Object surfaceChangedWaiter = new Object();

        private int glTextureId = NO_IMAGE;
        protected SurfaceTexture surfaceTexture = null;
        private final FloatBuffer glCubeBuffer;
        private final FloatBuffer glTextureBuffer;
        private IntBuffer glRgbBuffer;

        private int outputWidth;
        private int outputHeight;
        private int imageWidth;
        private int imageHeight;
        private int addedPadding;

        private final Queue<Runnable> runOnDraw;
        private final Queue<Runnable> runOnDrawEnd;
        private Rotation rotation;
        private boolean flipHorizontal;
        private boolean flipVertical;
        private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;

        public GPURenderer(final GPUImageFilter filter) {
            this.filter = filter;
            runOnDraw = new LinkedList<>();
            runOnDrawEnd = new LinkedList<>();

            glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            glCubeBuffer.put(CUBE).position(0);

            glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            setRotation(Rotation.NORMAL, false, false);
        }

//        @Override
//        public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
//            GLES20.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, 1);
//            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//            filter.ifNeedInit();
//        }
//
//        @Override
//        public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
//            outputWidth = width;
//            outputHeight = height;
//            GLES20.glViewport(0, 0, width, height);
//            GLES20.glUseProgram(filter.getProgram());
//            filter.onOutputSizeChanged(width, height);
//            adjustImageScaling();
//            synchronized (surfaceChangedWaiter) {
//                surfaceChangedWaiter.notifyAll();
//            }
//        }

        public void onDrawFrame(final GL10 gl) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            runAll(runOnDraw);
            filter.onDraw(glTextureId, glCubeBuffer, glTextureBuffer);
            runAll(runOnDrawEnd);
            if (surfaceTexture != null) {
                surfaceTexture.updateTexImage();
            }
        }


        private void runAll(Queue<Runnable> queue) {
            synchronized (queue) {
                while (!queue.isEmpty()) {
                    queue.poll().run();
                }
            }
        }

        public void onPreviewFrame(final byte[] data, final int width, final int height) {
            if (glRgbBuffer == null) {
                glRgbBuffer = IntBuffer.allocate(width * height);
            }
            if (runOnDraw.isEmpty()) {
                runOnDraw(new Runnable() {
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
        }

        public void setFilter(final GPUImageFilter filter) {
            runOnDraw(new Runnable() {

                @Override
                public void run() {
                    final GPUImageFilter oldFilter =   GPURenderer.this.filter;
                    GPURenderer.this.filter = filter;
                    if (oldFilter != null) {
                        oldFilter.destroy();
                    }
                    GPURenderer.this.filter.ifNeedInit();
                    GLES20.glUseProgram(GPURenderer.this.filter.getProgram());
                    GPURenderer.this.filter.onOutputSizeChanged(outputWidth, outputHeight);
                }
            });
        }

        public void deleteImage() {
            runOnDraw(new Runnable() {

                @Override
                public void run() {
                    GLES20.glDeleteTextures(1, new int[]{
                            glTextureId
                    }, 0);
                    glTextureId = NO_IMAGE;
                }
            });
        }

        public void setScaleType(GPUImage.ScaleType scaleType) {
            this.scaleType = scaleType;
        }

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

        protected void runOnDraw(final Runnable runnable) {
            synchronized (runOnDraw) {
                runOnDraw.add(runnable);
            }
        }

        protected void runOnDrawEnd(final Runnable runnable) {
            synchronized (runOnDrawEnd) {
                runOnDrawEnd.add(runnable);
            }
        }
    }

