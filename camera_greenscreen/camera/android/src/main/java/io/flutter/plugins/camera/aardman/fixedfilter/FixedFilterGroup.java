package io.flutter.plugins.camera.aardman.fixedfilter;

import static jp.co.cyberagent.android.gpuimage.GPUImageRenderer.CUBE;
import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

/**
     * Resembles a filter that consists of multiple filters applied after each
     * other.
     */
    public class FixedFilterGroup extends FixedBaseFilter {

        private List<FixedBaseFilter> filters;
        private List<FixedBaseFilter> mergedFilters;
        private int[] frameBuffers;
        private int[] frameBufferTextures;

        private final FloatBuffer glCubeBuffer;
        private final FloatBuffer glTextureBuffer;
        private final FloatBuffer glTextureFlipBuffer;

        /**
         * Instantiates a new FixedPipelineFilterGroup with no filters.
         */
        public FixedFilterGroup() {
            this(null);
        }

        /**
         * Instantiates a new FixedPipelineFilterGroup with the given filters.
         *
         * @param filters the filters which represent this filter
         */
        public FixedFilterGroup(List<FixedBaseFilter> filters) {
            this.filters = filters;
            if (this.filters == null) {
                this.filters = new ArrayList<>();
            } else {
                updateMergedFilters();
            }

            glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            glCubeBuffer.put(CUBE).position(0);

            glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            glTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

            float[] flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
            glTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            glTextureFlipBuffer.put(flipTexture).position(0);
        }

        public void addFilter(FixedBaseFilter aFilter) {
            if (aFilter == null) {
                return;
            }
            filters.add(aFilter);
            updateMergedFilters();
        }

        /*
         * (non-Javadoc)
         * @see jp.co.cyberagent.android.gpuimage.filter.FixedPipelineFilter#onInit()
         */
        @Override
        public void onInit() {
            super.onInit();
            for (FixedBaseFilter filter : filters) {
                filter.ifNeedInit();
            }
        }

        /*
         * (non-Javadoc)
         * @see jp.co.cyberagent.android.gpuimage.filter.FixedPipelineFilter#onDestroy()
         */
        @Override
        public void onDestroy() {
            destroyFramebuffers();
            for (FixedBaseFilter filter : filters) {
                filter.destroy();
            }
            super.onDestroy();
        }

        private void destroyFramebuffers() {
            if (frameBufferTextures != null) {
                GLES20.glDeleteTextures(frameBufferTextures.length, frameBufferTextures, 0);
                frameBufferTextures = null;
            }
            if (frameBuffers != null) {
                GLES20.glDeleteFramebuffers(frameBuffers.length, frameBuffers, 0);
                frameBuffers = null;
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * jp.co.cyberagent.android.gpuimage.filter.FixedPipelineFilter#onOutputSizeChanged(int,
         * int)
         */
        @Override
        public void onOutputSizeChanged(final int width, final int height) {
            super.onOutputSizeChanged(width, height);
            if (frameBuffers != null) {
                destroyFramebuffers();
            }

            int size = filters.size();
            for (int i = 0; i < size; i++) {
                filters.get(i).onOutputSizeChanged(width, height);
            }

            if (mergedFilters != null && mergedFilters.size() > 0) {
                size = mergedFilters.size();
                frameBuffers = new int[size - 1];
                frameBufferTextures = new int[size - 1];

                for (int i = 0; i < size - 1; i++) {
                    GLES20.glGenFramebuffers(1, frameBuffers, i);
                    GLES20.glGenTextures(1, frameBufferTextures, i);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTextures[i]);
                    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);
                    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                            GLES20.GL_TEXTURE_2D, frameBufferTextures[i], 0);

                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see jp.co.cyberagent.android.gpuimage.filter.FixedPipelineFilter#onDraw(int,
         * java.nio.FloatBuffer, java.nio.FloatBuffer)
         */
        @SuppressLint("WrongCall")
        @Override
        public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {
            if (!isInitialized() || frameBuffers == null || frameBufferTextures == null) {
                return;
            }
            if (mergedFilters != null) {
                int size = mergedFilters.size();
                int previousTexture = textureId;
                for (int i = 0; i < size; i++) {
                    FixedBaseFilter filter = mergedFilters.get(i);
                    boolean isNotLast = i < size - 1;
                    if (isNotLast) {
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffers[i]);
                        GLES20.glClearColor(0, 0, 0, 0);
                    }

                    if (i == 0) {
                        filter.onDraw(previousTexture, cubeBuffer, textureBuffer);
                    } else if (i == size - 1) {
                        filter.onDraw(previousTexture, glCubeBuffer, (size % 2 == 0) ? glTextureFlipBuffer : glTextureBuffer);
                    } else {
                        filter.onDraw(previousTexture, glCubeBuffer, glTextureBuffer);
                    }

                    if (isNotLast) {
                        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                        previousTexture = frameBufferTextures[i];
                    }
                }
            }
        }

        /**
         * Gets the filters.
         *
         * @return the filters
         */
        public List<FixedBaseFilter> getFilters() {
            return filters;
        }

        public List<FixedBaseFilter> getMergedFilters() {
            return mergedFilters;
        }

        public void updateMergedFilters() {
            if (filters == null) {
                return;
            }

            if (mergedFilters == null) {
                mergedFilters = new ArrayList<>();
            } else {
                mergedFilters.clear();
            }

            List<FixedBaseFilter> filters;
            for (FixedBaseFilter filter : this.filters) {
                if (filter instanceof FixedFilterGroup) {
                    ((FixedFilterGroup) filter).updateMergedFilters();
                    filters = ((FixedFilterGroup) filter).getMergedFilters();
                    if (filters == null || filters.isEmpty())
                        continue;
                    mergedFilters.addAll(filters);
                    continue;
                }
                mergedFilters.add(filter);
            }
        }
    }
