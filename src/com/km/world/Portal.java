package com.km.world;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Portal {

    private static int vaoid;
    private static int vboid;
    private static int tex_vboid;
    private static int idx_vboid;

    public int id;
    public int pid;

    public Vector3f target;

    static int poffset = 0;

    public Vector3f pos;

    static final float[] vertices = {
            -32f, 32f, 0,
            32f, 32f, 0,
            32f, -32f, 0,
            -32f, -32f, 0
    };

    static final float[] tex_coords = {
            0, 0,
            1, 0,
            1, 1,
            0, 1,
    };

    static final int[] indices = {
            0, 1, 2,
            2, 3, 0
    };

    public static void init() {

        vaoid = glGenVertexArrays();
        glBindVertexArray(vaoid);

        FloatBuffer posBuffer = null;
        FloatBuffer textCoordsBuffer = null;
        IntBuffer indicesBuffer = null;

        vboid = glGenBuffers();

        try {

            posBuffer = MemoryUtil.memAllocFloat(vertices.length);
            posBuffer.put(vertices).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboid);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Texture coordinates VBO
            tex_vboid = glGenBuffers();
            textCoordsBuffer = MemoryUtil.memAllocFloat(tex_coords.length);
            textCoordsBuffer.put(tex_coords).flip();
            glBindBuffer(GL_ARRAY_BUFFER, tex_vboid);
            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

            // Index VBO
            idx_vboid = glGenBuffers();
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idx_vboid);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

        } finally {

            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer);
            }
            if (textCoordsBuffer != null) {
                MemoryUtil.memFree(textCoordsBuffer);
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }

    }

    public Portal(Vector3f pos, int id) {
        this.pos = pos;
        this.id = id;
        this.pid = poffset++;
        target = new Vector3f(pos.x, pos.y - 20, 0);
    }


    public boolean glowing = false;

    int glow = 0;

    public Vector4f getGlow() {
        if (glow > 180)
            glow = 0;

        return new Vector4f(1.1f -  (glow++ % 60) / 120f, 1f -  (glow++ % 60) / 120f, 1.6f -  (glow++ % 60) / 720f, 1f);
    }

    public void render() {
        glBindVertexArray(vaoid);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glBindVertexArray(0);
    }
}
