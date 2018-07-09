package com.km;

import com.km.io.AttackInfoCache;
import org.joml.Vector2f;
import org.joml.Vector3f;
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
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Projectile {

    public static int vaoid;
    private static int vboid;
    private static int tex_vboid;
    private static int idx_vboid;

    public int id;

    public int frame;
    public int frames;

    public int range;
    public float speed;

    public float damage;

    public int eid;

    public String atlas;

    public boolean destroy = false;

    public Vector2f dir;
    public Vector3f pos;

    public Vector3f origin;


    static final float[] vertices = {
            -16f, 16f, 0,
            16f, 16f, 0,
            16f, -16f, 0,
            -16f, -16f, 0
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

    public Projectile() {}

    public Projectile(int eid, AttackInfoCache.AttackInfo info, Vector3f pos, Vector2f dir, float dmgmod) {
        this.id = info.id;
        this.eid = eid;
        this.atlas = info.atlas;
        this.speed = info.speed;
        this.frames = info.frames;
        this.range = info.range * 32;
        this.damage = info.damage * dmgmod;
        this.pos = pos;
        this.origin = new Vector3f(pos);
        this.dir = dir;
    }

    public void update() {
        pos.x += speed * dir.x;
        pos.y += speed * dir.y;

        if (Math.abs(pos.x - origin.x) >= range || Math.abs(pos.y - origin.y) >= range ) {
            destroy = true;
        }
    }

    int animCounter = 0;
    public void tickAnim() {
        animCounter++;
        if (animCounter >= 6) {
            frame++;
            animCounter = 0;

            if (frame >= frames)
                frame = 0;

        }
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

        tickAnim();
    }

    public static void cleanup() {
        glDisableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glDeleteBuffers(vboid);
        glDeleteBuffers(idx_vboid);
        glDeleteBuffers(tex_vboid);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoid);
    }

}
