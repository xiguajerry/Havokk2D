package com.km;

import com.km.gfx.TextObject;
import org.joml.Vector2f;
import org.joml.Vector3f;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Entity {

    public static final int DIR_UP = 3;
    public static final int DIR_LEFT = 1;
    public static final int DIR_RIGHT = 2;
    public static final int DIR_DOWN = 0;

    private static int vaoid;
    private static int vboid;
    private static int tex_vboid;
    private static int idx_vboid;

    private static int ID_OFFSET = 0;

    public int eid;

    double cooldown_start;
    double cast_start;
    double tele_start;
    double move_start;

    static final float MOVE_UPDATE_TIME = 1f / 1.5f;

    public boolean cooling = false;
    public boolean casting = false;
    public boolean teleporting = false;
    public boolean attackable = true;
    public boolean interactable = false;
    public boolean glowing = false;

    int glow = 0;

    public Vector4f getGlow() {
        if (glow > 180)
            glow = 0;

        return new Vector4f(1.1f -  (glow++ % 60) / 120f, 1f -  (glow++ % 60) / 120f, 1.6f -  (glow++ % 60) / 720f, 1f);
    }

    public float cooldown;

    public float speed = 1.0f;

    public float xp = 0;
    public int level = 1;

    private boolean moving = false;

    public String attack = "default";

    boolean attacking = false;
    int direction = DIR_DOWN;

    public float hitpoints = 500.0f;
    public float max_hitpoints = 500.0f;

    public boolean died = false;
    public boolean begin_cast = false;

    public int attackerId = 0;
    public boolean underAttack = false;
    public boolean tele_anim = false;
    public String name;

    public TextObject overhead;
    public String dialoguedir;

    private float hitmod = 1f;

    public final Vector2f movement = new Vector2f();

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

    public void addExp(float xp) {

        if (level >= MAX_LEVEL)
            return;

        this.xp += xp / Math.pow(3.5, (level / MAX_LEVEL));

        if (this.level != getLevelForExp(this.xp)) {
            level = getLevelForExp((int)this.xp);
            max_hitpoints = 500 + (500.0f * (float) Math.pow((level + 12), ((level + 120) / MAX_LEVEL)));
            hitpoints = max_hitpoints;
        }
    }

    public static double getExpForLevel(int level) {

        if (level > MAX_LEVEL)
            level = MAX_LEVEL;

        double xp = 0;

        for (int i = 0; i < level; i++) {
            xp += (i + 200) * (Math.pow(2, i / 7));
        }
        return xp;
    }

    static final int MAX_LEVEL = 100;

    public static int getLevelForExp(float xp) {

        double x = 0;

        for (int i = 0; i < MAX_LEVEL; i++) {
            x += (i + 200) * (Math.pow(2, i / 7));
            if (x >= xp)
                return i;
        }
        return MAX_LEVEL;
    }

    public void hit(float dmg, int id) {
        hitpoints -= dmg;

        if (hitpoints <= 0) {
            max_hitpoints = 500 + (500.0f * (float) Math.pow((level  + 12), ((level + 120) / MAX_LEVEL)));
            hitpoints = max_hitpoints;
            movement.zero();
            setPosition((float)(Math.random() * 64 * 64), (float)(Math.random() * 64 * 64));
            died = true;
            hitmod = 0;
            attacking = false;
            underAttack = false;
            teleporting = false;
            tele_anim = false;
            casting = false;
            casting_anim = false;
            return;
        }

        if (!(hitmod < 1f))
            hitmod = 0.6f;

        if (!underAttack) {
            attackerId = id;
            underAttack = true;
        }

    }

    public Vector3f pos;
    public Vector3f target_pos;

    public int texId;

    public int range = Havokk.WIDTH / 3;

    private boolean casting_anim = false;

    public boolean canMove = true;

    public Vector2f frame;

    public Entity(int tex, float speed, String attack, String name) {
        this.name = name;
        this.attack = attack;
        if (attack.equals("none"))
            attackable = false;

        texId = tex;
        pos = new Vector3f(0.0f, 0.0f, 0.0f);
        frame = new Vector2f(0, 0);
        this.speed = speed;
        eid = ID_OFFSET++;
        hitmod = 0;
        addExp((float)getExpForLevel(10));
    }

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

    public void playerUpdate() {

        if (hitpoints < max_hitpoints)
            hitpoints += 0.01 * level;

        double curr = System.nanoTime() / 1000000000.0;

        if (cooling) {

            if (curr - cooldown_start >= cooldown) {
                cooling = false;
                casting = false;
            }

            return;
        }

        if (casting) {

            if (curr - cast_start >= cooldown) {
                setCasting(false);
                setAttacking(true);
                cooling = false;
            }

            return;
        }

        if (teleporting) {

            if (curr - tele_start >= cooldown) {
                teleporting = false;
                hitmod = 0;
                startCooldown(600);
            }

            if (hitmod >= 0)
                updateMovement();

            return;
        }
    }

    public void update(double curr) {

        if (hitpoints < max_hitpoints)
            hitpoints += 0.01 * level;

        if (cooling) {

            if (curr - cooldown_start >= cooldown) {
                cooling = false;
                casting = false;
            }

            return;
        }

        if (casting) {

            if ((curr) - cast_start >= cooldown) {
                setCasting(false);
                setAttacking(true);
                cooling = false;
            }

            return;
        }

        if (teleporting) {

            if (curr - tele_start >= cooldown) {
                tele_anim = false;
                teleporting = false;
                hitmod = 0;
                movement.zero();
            //    move(0 ,0);
                startCooldown(600);
            }

            if (hitmod >= 0)
                updateMovement();

            return;
        }

        int roll = (int) (Math.random() * 30000);
        int roll2 = (int) (Math.random() * 4);

        Vector2f mov = new Vector2f();

        if (underAttack) {

            attackLogic();
            return;

        } else if (roll > 29000 && !attacking && canMove) {
            setAnimDir(roll2);

            if (roll > 29500)
                roll2 = 4;

            switch (roll2) {
                case 0:
                    mov.x = 0;
                    mov.y = -Havokk.MOVE_SPEED;
                    break;
                case 1:
                    mov.x = -Havokk.MOVE_SPEED;
                    mov.y = 0;
                    break;
                case 2:
                    mov.x = Havokk.MOVE_SPEED;
                    mov.y = 0;
                    break;
                case 3:
                    mov.x = 0;
                    mov.y = Havokk.MOVE_SPEED;
                    break;
                case 4:
                    mov.x = 0;
                    mov.y = 0;
                    break;
            }
            move(mov.x, mov.y);
        } else if (roll < 29 && !attacking && attackable) {
            begin_cast = true;
        }

        /*
        if (roll == 1212)
            startTeleport();
        */
    }

    public void setTarget(Vector3f pos) {
        target_pos = pos;
    }

    public void startCooldown(int cooldown) {

        cooldown_start = System.nanoTime() / 1000000000.0;

        this.cooldown = cooldown / 1000f;

        cooling = true;

        casting = false;

        setAttacking(false);

        move(0, 0);

    }

    public void startCast(int cooldown) {

        if (casting_anim || casting || teleporting || cooling || attacking)
            return;

        begin_cast = false;

        cast_start = System.nanoTime() / 1000000000.0;

        this.cooldown = cooldown / 1000f;

        casting = true;
        casting_anim = true;
        frame.x = 0;
    }

    public void startTeleport() {

        Vector2f mov = new Vector2f();

        switch (direction) {
            case DIR_DOWN:
                mov.x = 0;
                mov.y = -1;
                break;
            case DIR_LEFT:
                mov.x = -1;
                mov.y = 0;
                break;
            case DIR_RIGHT:
                mov.x = 1;
                mov.y = 0;
                break;
            case DIR_UP:
                mov.x = 0;
                mov.y = 1;
                break;
        }
        startTeleport(mov);
    }

    public void startTeleport(float x, float y) {
        startTeleport(new Vector2f(x, y));
    }

    public void startTeleport(Vector2f mov) {

        if (casting || teleporting || cooling || attacking)
            return;

        tele_anim = true;

        tele_start = System.nanoTime() / 1000000000.0;

        cooldown = 600f / 1000f;

        teleporting = true;

        mov.mul(14);

        move(mov.x, mov.y);

        hitmod = -1;
    }

    public void render() {

        if (teleporting)
            return;

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

    int animCounter = 0;
    public void tickAnim() {
        if (!attackable) {
            if (moving) {

                animCounter++;

                if (animCounter >= 10) {
                    frame.x = (frame.x + 1) % 3;
                    frame.y = direction;
                    animCounter = 0;
                }
            } else {
                frame.x = 1;
                frame.y = direction;
            }
        } else {
            animCounter++;

            if (animCounter >= 10) {
                if (casting_anim) {
                    if (frame.x + 1 >= 3)
                        casting_anim = false;
                }
                frame.x = (frame.x + 1) % 3;
                frame.y = direction + (casting_anim ? 4 : 0);
                animCounter = 0;
            }
        }
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

    public void move(float offsetX, float offsetY) {

        double curr = System.nanoTime() / 1000000000.0;

        if (underAttack && !teleporting) {
            if (eid == 0 || (curr - move_start > MOVE_UPDATE_TIME)) {
                move_start = curr;
                movement.x = offsetX * speed;
                movement.y = offsetY * speed;
            }
        } else {
            move_start = curr;
            movement.x = offsetX * speed;
            movement.y = offsetY * speed;
        }
    }

    public void updateMovement() {
        if (!attacking && !cooling) {
            pos.x += movement.x;
            pos.y += movement.y;

            moving = (movement.x != 0 || movement.y != 0);

        } else
            moving = false;
    }

    public void setPosition(float x, float y) {
        pos.x = x;
        pos.y = y;
    }

    public void setAnimDir(int dir) {
        if (dir != direction) {
            direction = dir;
            frame.y = direction + (casting_anim ? 4 : 0);

            if (!casting_anim)
                frame.x = 0;
        }
    }

    public void setCasting(boolean b) {
        casting = b;
    }

    public void setAttacking(boolean b) {
        attacking = b;
    }

    public float getDamage() {
        return 1f + (float) Math.pow(((level / 2)  + 1), ((level + 120) / MAX_LEVEL)) * 5f;
    }

    public float getHitMod() {
        if (hitmod < 1) {
            hitmod += 0.02f;
        }
        return hitmod;
    }

    public void attackLogic() {

        if (target_pos.distance(pos) > 64 * 12) {
            underAttack = false;
            return;
        }

        float xdiff = target_pos.x - pos.x;
        float ydiff = target_pos.y - pos.y;

        if (Math.abs(xdiff) <= Math.abs(ydiff)) {
            if (Math.abs(ydiff) <= range) {
                if (Math.abs(xdiff) < 32) {
                    if (ydiff > 0) {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_DOWN);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_UP);
                    } else {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_UP);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_DOWN);
                    }
                    begin_cast = true;
                } else {
                    if (ydiff > 1) {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_DOWN);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_UP);
                        move(0, Havokk.MOVE_SPEED);
                    } else {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_UP);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_DOWN);
                        move(0, -Havokk.MOVE_SPEED);
                    }
                }
            } else {
                if (ydiff > 1) {
                    if (hitpoints < (max_hitpoints / 10)) {
                        setAnimDir(DIR_DOWN);
                        startTeleport();
                        return;
                    }
                    setAnimDir(DIR_UP);
                    move(0, Havokk.MOVE_SPEED);
                } else {
                    if (hitpoints < (max_hitpoints / 10)) {
                        setAnimDir(DIR_UP);
                        startTeleport();
                        return;
                    }
                    setAnimDir(DIR_DOWN);
                    move(0, -Havokk.MOVE_SPEED);
                }
            }
        } else {
            if (Math.abs(ydiff) <= range) {
                if (Math.abs(ydiff) < 32) {
                    if (xdiff > 0) {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_LEFT);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_RIGHT);
                    } else {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_RIGHT);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_LEFT);
                    }
                    begin_cast = true;
                } else {
                    if (xdiff > 1) {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_LEFT);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_RIGHT);
                        move(Havokk.MOVE_SPEED, 0);
                    } else {
                        if (hitpoints < (max_hitpoints / 10)) {
                            setAnimDir(DIR_RIGHT);
                            startTeleport();
                            return;
                        }
                        setAnimDir(DIR_LEFT);
                        move(-Havokk.MOVE_SPEED, 0);
                    }
                }
            } else {
                if (xdiff > 1) {
                    if (hitpoints < (max_hitpoints / 10)) {
                        setAnimDir(DIR_LEFT);
                        startTeleport();
                        return;
                    }
                    setAnimDir(DIR_RIGHT);
                    move(Havokk.MOVE_SPEED, 0);
                } else {
                    if (hitpoints < (max_hitpoints / 10)) {
                        setAnimDir(DIR_RIGHT);
                        startTeleport();
                        return;
                    }
                    setAnimDir(DIR_LEFT);
                    move(-Havokk.MOVE_SPEED, 0);
                }
            }
        }

    }

}