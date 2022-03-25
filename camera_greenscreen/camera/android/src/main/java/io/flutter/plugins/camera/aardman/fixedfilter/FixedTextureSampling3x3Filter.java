package io.flutter.plugins.camera.aardman.fixedfilter;

import android.opengl.GLES20;

public class FixedTextureSampling3x3Filter extends FixedBaseFilter {

    public static final String THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform highp float texelWidth; \n" +
            "uniform highp float texelHeight; \n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 topLeftTextureCoordinate;\n" +
            "varying vec2 topRightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 bottomTextureCoordinate;\n" +
            "varying vec2 bottomLeftTextureCoordinate;\n" +
            "varying vec2 bottomRightTextureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "\n" +
            "    vec2 widthStep = vec2(texelWidth, 0.0);\n" +
            "    vec2 heightStep = vec2(0.0, texelHeight);\n" +
            "    vec2 widthHeightStep = vec2(texelWidth, texelHeight);\n" +
            "    vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);\n" +
            "\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "    leftTextureCoordinate = inputTextureCoordinate.xy - widthStep;\n" +
            "    rightTextureCoordinate = inputTextureCoordinate.xy + widthStep;\n" +
            "\n" +
            "    topTextureCoordinate = inputTextureCoordinate.xy - heightStep;\n" +
            "    topLeftTextureCoordinate = inputTextureCoordinate.xy - widthHeightStep;\n" +
            "    topRightTextureCoordinate = inputTextureCoordinate.xy + widthNegativeHeightStep;\n" +
            "\n" +
            "    bottomTextureCoordinate = inputTextureCoordinate.xy + heightStep;\n" +
            "    bottomLeftTextureCoordinate = inputTextureCoordinate.xy - widthNegativeHeightStep;\n" +
            "    bottomRightTextureCoordinate = inputTextureCoordinate.xy + widthHeightStep;\n" +
            "}";

    private int uniformTexelWidthLocation;
    private int uniformTexelHeightLocation;

    private boolean hasOverriddenImageSizeFactor = false;
    private float texelWidth;
    private float texelHeight;
    private float lineSize = 1.0f;

    public FixedTextureSampling3x3Filter() {
        this(NO_FILTER_VERTEX_SHADER);
    }

    public FixedTextureSampling3x3Filter(final String fragmentShader) {
        super(THREE_X_THREE_TEXTURE_SAMPLING_VERTEX_SHADER, fragmentShader);
    }

    @Override
    public void onInit() {
        super.onInit();
        uniformTexelWidthLocation = GLES20.glGetUniformLocation(getProgram(), "texelWidth");
        uniformTexelHeightLocation = GLES20.glGetUniformLocation(getProgram(), "texelHeight");
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
        if (texelWidth != 0) {
            updateTexelValues();
        }
    }

    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        if (!hasOverriddenImageSizeFactor) {
            setLineSize(lineSize);
        }
    }

    public void setTexelWidth(final float texelWidth) {
        hasOverriddenImageSizeFactor = true;
        this.texelWidth = texelWidth;
        setFloat(uniformTexelWidthLocation, texelWidth);
    }

    public void setTexelHeight(final float texelHeight) {
        hasOverriddenImageSizeFactor = true;
        this.texelHeight = texelHeight;
        setFloat(uniformTexelHeightLocation, texelHeight);
    }

    public void setLineSize(final float size) {
        lineSize = size;
        texelWidth = size / getOutputWidth();
        texelHeight = size / getOutputHeight();
        updateTexelValues();
    }

    private void updateTexelValues() {
        setFloat(uniformTexelWidthLocation, texelWidth);
        setFloat(uniformTexelHeightLocation, texelHeight);
    }

}
