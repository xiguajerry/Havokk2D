package com.km;

import com.km.io.AttackInfoCache;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class Arrow extends Projectile {

    public Arrow(int eid, AttackInfoCache.AttackInfo info, Vector3f pos, Vector2f dir, float dmgmod) {
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

        if (dir.x == 1)
            frame = Entity.DIR_RIGHT;
        else if (dir.x == -1)
            frame = Entity.DIR_LEFT;
        else if (dir.y == 1)
            frame = Entity.DIR_UP;
        else if (dir.y == -1)
            frame = Entity.DIR_DOWN;

    }

    @Override
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
