package ru.hse.alyokhina.juliafractal;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import glm.mat.Mat4x4;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import ru.hse.alyokhina.Fractal;
import ru.hse.alyokhina.juliafractal.framework.Semantic;
import uno.glsl.Program;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_REPEAT;
import static com.jogamp.opengl.GL.GL_RGB;
import static com.jogamp.opengl.GL.GL_RGBA32F;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNPACK_ALIGNMENT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_STREAM_DRAW;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_DEPTH;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2GL3.GL_TEXTURE_1D;
import static com.jogamp.opengl.math.FloatUtil.sin;
import static glm.GlmKt.glm;
import static java.awt.Color.RED;
import static uno.buffer.UtilKt.destroyBuffers;
import static uno.gl.GlErrorKt.checkError;

/**
 * Created by elect on 04/03/17.
 */

public class JuliaFractal extends Fractal {

    private float[] vertexData = {
            -10, -10, 0, 0, 0,
            -10, +10, 0, 0, 0,
            +10, -10, 0, 0, 0,
            +10, +10, 0, 0, 0};

    private float[] colorData = {
            1.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 0.0f,
            0.8862745098039215f, 0.3411764705882353f, 0.11764705882352941f,
            1.0f, 0.4980392156862745f, 0.0f,
            1.0f, 0.7843137254901961f, 0.0f,
            1.0f, 1.0f, 0.0f,
            0.7843137254901961f, 1.0f, 0.0f,
            0.5882352941176471f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.5882352941176471f, 0.7490196078431373f, 0.2f,
            0.0f, 0.7843137254901961f, 1.0f,
            0.0f, 0.5882352941176471f, 1.0f,
            0.0f, 0.39215686274509803f, 1.0f,
            0.0f, 0.19607843137254902f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.29411764705882354f, 0.0f, 0.5098039215686274f
    };

    private short[] elementData = {0, 2, 1, 1, 2, 3};
    private final int width;
    private final int height;

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    private Program program;

    private boolean auto = true;
    private float thetaX = 0;
    private float thetaY = 0;
    private int maxIter = 100;
    private float zoom = 2;
    private float cX = -0.7f;
    private float cY = 0.27015f;
    private float moveX = 0;
    private float moveY = 0;
    private float R = 4;

    private int maxIterUniform;
    private int zoomUniform;
    private int cXUniform;
    private int cYUniform;
    private int moveXUniform;
    private int moveYUniform;
    private int RUniform;
    private int colorsUniform;
    private int colorsCountUniform;

    private int textureId;


    public JuliaFractal(final int w,
                        final int h) {
        width = w;
        height = h;
    }


    @Override
    public void incZoom(float del) {
        zoom *= del;
    }

    @Override
    public void move(float moveX, float moveY) {
        this.moveX -= 2f * moveX / width / zoom;
        this.moveY += 2f * moveY / height / zoom;
    }

    @Override
    public void setMaxIter(int maxIter) {
        this.maxIter = maxIter;
    }

    @Override
    public int getMaxIter() {
        return maxIter;
    }

    @Override
    public void setR(float R) {
        this.R = R;
    }

    @Override
    public void revertAuto() {
        this.auto = !auto;
    }

    public float getR() {
        return R;
    }

    @Override
    public boolean getAuto() {
        return auto;
    }

    @Override
    public void setParameterX(float x) {
        this.cX = x;
    }

    @Override
    public float getParameterX() {
        return cX;
    }

    @Override
    public void setParameterY(float y) {
        this.cY = y;
    }

    @Override
    public float getParameterY() {
        return cY;
    }


    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        initBuffers(gl);

        initVertexArray(gl);

        initProgram(gl);
        maxIterUniform = gl.glGetUniformLocation(program.name, "maxIter");
        zoomUniform = gl.glGetUniformLocation(program.name, "zoom");
        cXUniform = gl.glGetUniformLocation(program.name, "cX");
        cYUniform = gl.glGetUniformLocation(program.name, "cY");
        moveXUniform = gl.glGetUniformLocation(program.name, "moveX");
        moveYUniform = gl.glGetUniformLocation(program.name, "moveY");
        RUniform = gl.glGetUniformLocation(program.name, "R");
        colorsUniform = gl.glGetUniformLocation(program.name, "colors");
        colorsCountUniform = gl.glGetUniformLocation(program.name, "colorsCount");

        textureId = initTexture(gl);
        gl.glEnable(GL_DEPTH_TEST);
    }

    private void initBuffers(GL3 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * Short.BYTES, elementBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);


        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferData(GL_UNIFORM_BUFFER, Mat4x4.SIZE * 2, null, GL_STREAM_DRAW);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, bufferName.get(Buffer.GLOBAL_MATRICES));


        destroyBuffers(vertexBuffer, elementBuffer);

        checkError(gl, "initBuffers");
    }

    private int initTexture(GL3 gl) {
        IntBuffer textures = GLBuffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, textures);

        gl.glBindTexture(GL_TEXTURE_1D, textures.get(0));

        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        FloatBuffer colors = GLBuffers.newDirectFloatBuffer(colorData);

        gl.glTexImage1D(
                GL_TEXTURE_1D,
                0,
                GL_RGBA32F,
                colors.capacity() / 3,
                0,
                GL_RGB,
                GL_FLOAT,
                colors
        );

        // texture sampling/filtering operation.
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        gl.glBindTexture(GL_TEXTURE_1D, 0);

        return textures.get(0);
    }

    private void initVertexArray(GL3 gl) {

        gl.glGenVertexArrays(1, vertexArrayName);
        gl.glBindVertexArray(vertexArrayName.get(0));
        {
            gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            {
                int stride = Vec2.SIZE + Vec3.SIZE;
                int offset = 0;

                gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
                gl.glVertexAttribPointer(Semantic.Attr.POSITION, Vec2.length, GL_FLOAT, false, stride, offset);

                offset = Vec2.SIZE;
                gl.glEnableVertexAttribArray(Semantic.Attr.COLOR);
                gl.glVertexAttribPointer(Semantic.Attr.COLOR, Vec3.length, GL_FLOAT, false, stride, offset);
            }
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl.glBindVertexArray(0);

        checkError(gl, "initVao");
    }

    private void initProgram(GL3 gl) {

        program = new Program(gl, getClass(), "shaders/gl3", "hello-triangle.vert", "hello-triangle.frag", "model");

        int globalMatricesBI = gl.glGetUniformBlockIndex(program.name, "GlobalMatrices");

        if (globalMatricesBI == -1) {
            System.err.println("block index 'GlobalMatrices' not found!");
        }
        gl.glUniformBlockBinding(program.name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES);

        checkError(gl, "initProgram");
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        // view matrix
        {
            gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
            gl.glBufferSubData(GL_UNIFORM_BUFFER, Mat4x4.SIZE, Mat4x4.SIZE, matBuffer);
            gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f)
                .put(1, .33f).put(2, 0.66f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f));

        gl.glUseProgram(program.name);
        gl.glBindVertexArray(vertexArrayName.get(0));
        gl.glUniform1i(maxIterUniform, maxIter);
        gl.glUniform1f(zoomUniform, zoom);
        gl.glUniform1f(cXUniform, cX);
        gl.glUniform1f(cYUniform, cY);
        gl.glUniform1f(moveXUniform, moveX);
        gl.glUniform1f(moveYUniform, moveY);
        gl.glUniform1f(RUniform, R);

        int currentTextureUnit_ = 0;
        gl.glUniform1i(colorsUniform, currentTextureUnit_);

        gl.glActiveTexture(GL_TEXTURE0 + currentTextureUnit_);
        gl.glBindTexture(GL_TEXTURE_1D, textureId);

        gl.glUniform1i(colorsCountUniform, colorData.length / 3);

        {


            Mat4x4 model = new Mat4x4();
            model
                    .scale(0.5f)
                    .to(matBuffer);
            gl.glUniformMatrix4fv(program.get("model"), 1, false, matBuffer);
        }

        gl.glDrawElements(GL_TRIANGLES, elementData.length, GL_UNSIGNED_SHORT, 0);

        gl.glUseProgram(0);
        gl.glBindVertexArray(0);
        if (auto) {
            thetaX += 0.01;
            thetaY += 0.001;
            cX = sin(thetaX);
            cY = sin(thetaY);
        }
        checkError(gl, "display");
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL3 gl = drawable.getGL().getGL3();

        glm.ortho(-1f, 1f, -1f, 1f, 1f, -1f).to(matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, Mat4x4.SIZE, matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        gl.glDeleteProgram(program.name);
        gl.glDeleteVertexArrays(1, vertexArrayName);
        gl.glDeleteBuffers(Buffer.MAX, bufferName);

        destroyBuffers(vertexArrayName, bufferName, matBuffer, clearColor, clearDepth);
    }


    private interface Buffer {

        int VERTEX = 0;
        int ELEMENT = 1;
        int GLOBAL_MATRICES = 2;
        int MAX = 3;
    }

}