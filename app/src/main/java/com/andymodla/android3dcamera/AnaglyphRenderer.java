package com.andymodla.android3dcamera;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//  NOT WORKNG experimental lots of work to do here and learn about
public class AnaglyphRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "AnaglyphRenderer";
    private int leftTextureId, rightTextureId;
    private SurfaceTexture leftSurfaceTexture, rightSurfaceTexture;
    private Surface leftSurface, rightSurface;
    private int shaderProgram;
    private int aPositionHandle, aTexCoordHandle;
    private int uTextureHandle;
    private FloatBuffer vertexBuffer, texCoordBuffer;
    private boolean isAnaglyphMode = true;
    private GLSurfaceView glSurfaceView;
    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTexCoord;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = aPosition;\n" +
                    "    vTexCoord = aTexCoord;\n" +
                    "}\n";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES uTexture;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
                    "}\n";
    private static final float[] VERTICES = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };
    private static final float[] TEX_COORDS = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "AnaglyphRenderer.onSurfaceCreated()");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        // Load shaders and create program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShader);
        GLES20.glAttachShader(shaderProgram, fragmentShader);
        GLES20.glLinkProgram(shaderProgram);
        // Get handles
        aPositionHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition");
        aTexCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        uTextureHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        // Setup vertex buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);
        // Setup texture coordinate buffer
        ByteBuffer tcbb = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        tcbb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tcbb.asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS);
        texCoordBuffer.position(0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (leftSurfaceTexture != null && rightSurfaceTexture != null) {
            leftSurfaceTexture.updateTexImage();
            rightSurfaceTexture.updateTexImage();
            GLES20.glUseProgram(shaderProgram);
            GLES20.glEnableVertexAttribArray(aPositionHandle);
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(aTexCoordHandle);
            GLES20.glVertexAttribPointer(aTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glUniform1i(uTextureHandle, 0);
            if (isAnaglyphMode) {
                // ANAGLYPH MODE: Use OpenGL ColorMask
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                // First pass: Draw left eye with RED channel only
                GLES20.glColorMask(true, false, false, false); // Only red
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, leftTextureId);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                // Second pass: Draw right eye with GREEN and BLUE channels only
                GLES20.glColorMask(false, true, true, false); // Only green and blue
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, rightTextureId);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                // Reset color mask
                GLES20.glColorMask(true, true, true, true);
            } else {
                // SIDE-BY-SIDE MODE: Normal rendering
                GLES20.glColorMask(true, true, true, true);
                // Draw left camera on left half
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, leftTextureId);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                // Draw right camera on right half
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, rightTextureId);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            }
            GLES20.glDisableVertexAttribArray(aPositionHandle);
            GLES20.glDisableVertexAttribArray(aTexCoordHandle);
        }
    }

    public void setupSurfaces() {
        int[] leftTextures = new int[1];
        GLES20.glGenTextures(1, leftTextures, 0);
        leftTextureId = leftTextures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, leftTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        leftSurfaceTexture = new SurfaceTexture(leftTextureId);
        leftSurface = new Surface(leftSurfaceTexture);
        int[] rightTextures = new int[1];
        GLES20.glGenTextures(1, rightTextures, 0);
        rightTextureId = rightTextures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, rightTextureId);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        rightSurfaceTexture = new SurfaceTexture(rightTextureId);
        rightSurface = new Surface(rightSurfaceTexture);
        leftSurfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
            if (glSurfaceView != null) {
                glSurfaceView.requestRender();
            }
        });
        rightSurfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
            if (glSurfaceView != null) {
                glSurfaceView.requestRender();
            }
        });
    }

    public Surface getLeftSurface() {
        return leftSurface;
    }

    public Surface getRightSurface() {
        return rightSurface;
    }

    public void toggleDisplayMode() {
        isAnaglyphMode = !isAnaglyphMode;
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
    }

    public boolean isAnaglyphMode() {
        return isAnaglyphMode;
    }

    public void setGLSurfaceView(GLSurfaceView view) {
        this.glSurfaceView = view;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compilation error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}