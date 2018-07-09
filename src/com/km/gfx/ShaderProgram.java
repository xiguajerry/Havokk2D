package com.km.gfx;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {

    private final int programId;

    private int vertexShaderId;

    private int fragmentShaderId;

    private final Map<String, Integer> uniforms;

    public ShaderProgram() throws Exception {
        programId = glCreateProgram();
        if (programId == 0)
            throw new Exception("Could not create shader");

        uniforms = new HashMap<>();
    }

    public void createUniform(String uniformName) throws Exception {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            throw new Exception("Could not find uniform:" + uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }

    public void setUniform(String uniformName, Vector4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer fb = stack.mallocFloat(4);
            value.get(fb);
            glUniform4fv(uniforms.get(uniformName), fb);
        }
    }

    public void setUniform(String uniformName, int value) {
        glUniform1i(uniforms.get(uniformName), value);
    }

    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Dump the matrix into a float buffer
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(uniforms.get(uniformName), false, fb);
        }
    }

    public void createVertexShader(String code) throws Exception {
        vertexShaderId = createShader(code, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String code) throws Exception {
        fragmentShaderId = createShader(code, GL_FRAGMENT_SHADER);
    }

    protected int createShader(String code, int shaderType) throws Exception {

        int shaderId = glCreateShader(shaderType);

        if (shaderId == 0)
            throw new Exception("Error creating shader");

        glShaderSource(shaderId, code);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0)
            throw new Exception("Error compiling shader\n" + glGetShaderInfoLog(shaderId, 1024));

        glAttachShader(programId, shaderId);

        return shaderId;
    }

    public void link() throws Exception {
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0)
            throw new Exception("Error linking shader code\n" + glGetShaderInfoLog(programId, 1024));


        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + glGetProgramInfoLog(programId, 1024));
        }

    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();

        if (programId != 0)
            glDeleteProgram(programId);
    }

    public static String loadShader(String fn) throws IOException {

        System.out.println("Loading shader code....... " + fn);

        BufferedReader br = new BufferedReader(new FileReader("./res/shaders/" + fn));

        try {

            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            return sb.toString();

        } finally {
            br.close();
        }
    }

}
