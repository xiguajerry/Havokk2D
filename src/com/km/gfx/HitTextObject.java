package com.km.gfx;

import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class HitTextObject extends TextObject {

    private static final double LIFETIME = 0.4;
    private double start;

    public HitTextObject(String s, Vector3f pos) {
        text = s;
        this.pos = pos;
        start = System.nanoTime() / 1000000000.0;
    }

    public HitTextObject(int i, Vector3f pos) {
        text = Integer.toString(i);
        this.pos = pos;
        start = System.nanoTime() / 1000000000.0;
    }

    public HitTextObject(float i, Vector3f pos) {
        text = Integer.toString((int)i);
        this.pos = pos;
        start = System.nanoTime() / 1000000000.0;
    }

    public Vector3f getPos() {
        double curr = System.nanoTime() / 1000000000.0;
        return new Vector3f((float)(pos.x  + ((curr - start) * 12)), (float)(pos.y  - ((curr - start) * 10)), 0);
    }

    @Override
    public void render() {
        if (destroy)
            return;

        if (System.nanoTime() / 1000000000.0 - start >= LIFETIME) {
            destroy = true;
            return;
        }

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

    public float getHitMod() {
        return (float)(1 -((System.nanoTime() / 1000000000.0 - start) / LIFETIME));
    }

}
