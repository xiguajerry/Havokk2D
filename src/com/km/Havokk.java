package com.km;

import com.km.gfx.*;

import com.km.io.AnimInfoCache;
import com.km.io.AttackInfoCache;
import com.km.io.EntityInfoCache;
import com.km.world.MapLoader;
import com.km.world.Portal;
import com.km.world.Tile;
import com.km.world.WorldMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import org.joml.Vector4f;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Havokk {

    private long window;

    public static int WIDTH = 640;
    public static int HEIGHT = 480;

    public static final float MOVE_SPEED = 3f;

    public static final float TILESIZE = 64.0f;

    private boolean dirlock = false;

    private static final int MAXFPS = 60;
    private final double TARGET_TIME = 1.0 / MAXFPS;

    private Vector3f cameraLocation = new Vector3f(0, 0, 0);

    private Matrix4f projection;
    private Matrix4f world;
    private Matrix4f viewMatrix;

    public final Vector2f delta = new Vector2f();

    int fps = 0;

    int currmaxlevel = 0;
    int currmaxdmg = 0;

    private ShaderProgram shaderProgram;

    private TextureCache textureCache;

    private EntityInfoCache entityInfoCache;

    private AttackInfoCache attackInfoCache;
    private AnimInfoCache animInfoCache;

    private Map<String, List<Entity>> entities;

    private List<Projectile> projectiles;
    private List<Projectile> dest_projectiles;

    private List<TextObject> textObjects;
    private List<TextObject> dest_texts;

    private List<TextObject> overheadTexts;
    private List<TextObject> dest_overtexts;

    private List<HitTextObject> hitTexts;
    private List<HitTextObject> dest_hittexts;

    private List<StaticAnim> anims;
    private List<StaticAnim> dest_anims;

    Entity player;

    TextObject text_fps;
    TextObject text_max;
    TextObject text_loc;
    TextObject text_hp;
    TextObject text_count;
    TextObject tile_count;
    TextObject proj_count;
    TextObject text_stats;
    TextObject text_action;

    ArrayList<TextObject> chats;

    TextObject chat;

    int rendercount = 0;
    int projectilecount = 0;
    int tilecount = 0;
    int rendertotal = 0;
    int projectiletotal = 0;
    int tiletotal = 0;
    int interactableid = -1;
    int portalid = -1;
    boolean interacting = false;
    boolean showChat = false;
    boolean showAction = false;

    WorldMap map;

    public static void main(String[] args) {
        System.out.println("Initialising......");
        new Havokk().run();
    }

    public void run() {
        System.out.println("LWJGL Version: " + Version.getVersion());

        init();

        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {

        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialise GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        WIDTH = vidmode.width();
        HEIGHT = vidmode.height();

        window = glfwCreateWindow(WIDTH, HEIGHT, "Havokk", glfwGetPrimaryMonitor(), NULL);

        if (window == NULL)
            throw new RuntimeException("Failed to create window");

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);
        });

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window);

        glfwSwapInterval(1);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try {
            initShaders();
        } catch (Exception e) {
            e.printStackTrace();
        }

        textureCache = new TextureCache();
        animInfoCache = new AnimInfoCache();

        for (AnimInfoCache.AnimInfo a : animInfoCache.anims.values()) {
            textureCache.getAtlas(a.atlas);
        }

        projection = new Matrix4f().setOrtho2D(-WIDTH / 2, WIDTH / 2, -HEIGHT / 2, HEIGHT / 2);

        viewMatrix = new Matrix4f().setTranslation(new Vector3f(0));

        world = new Matrix4f().setTranslation(new Vector3f(0));
        world.scale(1f);

        map = MapLoader.loadMap("map_home", textureCache.getAtlas("map_master"));

        tiletotal = map.height * map.width;

        attackInfoCache = new AttackInfoCache();

        //chats = new ArrayList<>();

        for (AttackInfoCache.AttackInfo a : attackInfoCache.attacks.values())
            textureCache.getAtlas(a.atlas);

        Projectile.init();
        projectiles = new ArrayList<>();
        dest_projectiles = new ArrayList<>();

        StaticAnim.init();
        anims = new ArrayList<>();
        dest_anims = new ArrayList<>();

        TextObject.init(textureCache.getAtlas("font_texture"));

        textObjects = new ArrayList<>();
        dest_texts = new ArrayList<>();

        hitTexts = new ArrayList<>();
        dest_hittexts = new ArrayList<>();

        overheadTexts = new ArrayList<>();
        dest_overtexts = new ArrayList<>();

        generateEntities();

        glfwShowWindow(window);

    }

    private void initShaders() throws Exception {
        System.out.println("Initialising shaders.......");
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ShaderProgram.loadShader("vertex.vs"));
        shaderProgram.createFragmentShader(ShaderProgram.loadShader("fragment.fs"));
        shaderProgram.link();

        shaderProgram.createUniform("projection");
        shaderProgram.createUniform("image");
        shaderProgram.createUniform("frame");
        shaderProgram.createUniform("hit");

        System.out.println("Initialised shaders.......");
    }

    public void loop() {

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        double start = 0;
        double last = System.nanoTime() / 1000000000.0;
        double elapsed = 0;
        double unprocessed = 0;

        double frameTime = 0;
        int frames = 0;

        boolean render;

        setProjection();

        while (!glfwWindowShouldClose(window)) {

            render = false;

            start = System.nanoTime() / 1000000000.0;
            elapsed = start - last;
            last = start;

            unprocessed += elapsed;
            frameTime += elapsed;

            while (unprocessed >= TARGET_TIME) {

                unprocessed -= TARGET_TIME;
                render = true;

                input();

                update();

                if (frameTime >= 1.0) {
                    frameTime = 0;
                    fps = frames;
                    frames = 0;
                }
            }

            if (render) {
                draw();
                frames++;
            } else {
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        TextObject.cleanup();
        Entity.cleanup();
        Projectile.cleanup();
        StaticAnim.cleanup();
    }

    private void input() {

        delta.set(0, 0);

        if (player.casting || player.cooling || player.teleporting || player.attacking)
            return;

        if (isKeyPressed(GLFW_KEY_KP_ADD))
            player.addExp(1000f * player.level);

        if (isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            dirlock = true;
        } else {
            dirlock = false;
        }

        if (!player.attacking) {

            if (isKeyPressed(GLFW_KEY_SPACE)) {
                player.startCast(attackInfoCache.get(player.attack).cast);
                return;
            }

            if (isKeyPressed(GLFW_KEY_UP)) {
                delta.y = 1;
                delta.x = 0;

                if (!dirlock)
                    player.setAnimDir(Entity.DIR_UP);

                if (isKeyPressed(GLFW_KEY_LEFT_ALT)) {
                    player.startTeleport(delta);
                    createStaticAnim("teleport", player.pos, true);
                    return;
                }

            } else if (isKeyPressed(GLFW_KEY_DOWN)) {
                delta.y = -1;
                delta.x = 0;

                if (!dirlock)
                    player.setAnimDir(Entity.DIR_DOWN);

                if (isKeyPressed(GLFW_KEY_LEFT_ALT)) {
                    player.startTeleport(delta);
                    createStaticAnim("teleport", player.pos, true);
                    return;
                }

            } else if (isKeyPressed(GLFW_KEY_LEFT)) {
                delta.y = 0;
                delta.x = -1;

                if (!dirlock)
                    player.setAnimDir(Entity.DIR_LEFT);

                if (isKeyPressed(GLFW_KEY_LEFT_ALT)) {
                    player.startTeleport(delta);
                    createStaticAnim("teleport", player.pos, true);
                    return;
                }

            } else if (isKeyPressed(GLFW_KEY_RIGHT)) {
                delta.y = 0;
                delta.x = 1;

                if (!dirlock)
                    player.setAnimDir(Entity.DIR_RIGHT);

                if (isKeyPressed(GLFW_KEY_LEFT_ALT)) {
                    player.startTeleport(delta);
                    createStaticAnim("teleport", player.pos, true);
                    return;
                }

            } else if (isKeyPressed(GLFW_KEY_LEFT_ALT)) {
                player.startTeleport();
                createStaticAnim("teleport", player.pos, true);
            } else if (isKeyPressed(GLFW_KEY_ENTER)) {
                if (interactableid != -1 && (!interacting || showAction)) {
                    beginInteraction(interactableid);
                    interacting = true;
                } else if (portalid != -1 && (!interacting || showAction)) {
                    beginPortal(portalid);
                    interacting = true;
                }
            }
        }
    }

    double actiontimer;

    void beginInteraction(int entityid) {
        Entity e = getEntityForId(entityid);

        if (e != null) {

            if (e.dialoguedir.equals("none"))
                return;

            chat = new TextObject(entityInfoCache.getRandomQuest(e.dialoguedir), e.pos);
            showChat = true;
            e.movement.zero();
            e.canMove = false;

            float xdiff = e.pos.x - player.pos.x;
            float ydiff = e.pos.y - player.pos.y;

            if (Math.abs(xdiff) <= Math.abs(ydiff)) {
                if (ydiff > 0) {
                    e.setAnimDir(Entity.DIR_DOWN);
                    player.setAnimDir(Entity.DIR_UP);
                } else {
                    e.setAnimDir(Entity.DIR_UP);
                    player.setAnimDir(Entity.DIR_DOWN);
                }
            } else {
                if (xdiff > 0) {
                    e.setAnimDir(Entity.DIR_LEFT);
                    player.setAnimDir(Entity.DIR_RIGHT);
                } else {
                    e.setAnimDir(Entity.DIR_RIGHT);
                    player.setAnimDir(Entity.DIR_LEFT);
                }
            }
        }

        showAction = false;

        actiontimer = System.nanoTime() / 1000000000.0;
    }

    void beginPortal(int id) {

        Portal p = getPortalForId(id);

        if (p != null) {
            p.glowing = false;
            player.setPosition(p.target.x, p.target.y - 12 * 64);
        }

        showAction = false;

        actiontimer = System.nanoTime() / 1000000000.0;
    }

    Portal getPortalForId(int id) {
        for (Portal p : map.getPortals())
            if (p.pid == id)
                return p;

        return null;
    }

    private void attack(Entity player) {

        Vector2f dir;

        switch (player.direction) {
            case Entity.DIR_UP:
                dir = new Vector2f(0, 1);
                break;
            case Entity.DIR_DOWN:
                dir = new Vector2f(0, -1);
                break;
            case Entity.DIR_RIGHT:
                dir = new Vector2f(1, 0);
                break;
            case Entity.DIR_LEFT:
                dir = new Vector2f(-1, 0);
                break;
            default:
                dir = new Vector2f(0, 0);
        }
        createProjectile(player.eid, player.attack, new Vector3f(player.pos), dir, player.getDamage());
        player.startCooldown(attackInfoCache.get(player.attack).cooldown);
    }

    Matrix4f scale = new Matrix4f().scale(1.0f / 3.0f, 1.0f / 8.0f, 0);

    private Matrix4f getEntityTextureTranslation(Vector2f frame) {
        return scale.translate(frame.x, frame.y, 0, new Matrix4f());
    }

    Matrix4f nascale = new Matrix4f().scale(1.0f / 3.0f, 1.0f / 4.0f, 0);

    private Matrix4f getNonAttackableEntityTextureTranslation(Vector2f frame) {
        return nascale.translate(frame.x, frame.y, 0, new Matrix4f());
    }

    private void draw() {

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shaderProgram.bind();

        shaderProgram.setUniform("hit", new Vector4f(1, 1, 1, 1f));

        renderWorld();

        //drawOverheadText();

        shaderProgram.setUniform("hit", new Vector4f(1, 1, 1, 1f));

        renderEntities();

        shaderProgram.setUniform("hit", new Vector4f(1, 1, 1, 1f));

        renderProjectiles();

        renderAnims();

        renderWorldTop();

        drawfps();
        drawloc();
        drawcount();
        drawprojcount();
        drawtilecount();
        //drawmaxlevel();
        drawhp();
        drawstats();
        drawTextObs();
        drawHitTexts();

        shaderProgram.setUniform("hit", new Vector4f(1, 1, 1, 1f));

        drawchat();
        drawAction();

        glfwSwapBuffers(window);

        glfwPollEvents();

        shaderProgram.unbind();
    }

    private void renderEntities() {
        rendercount = 0;

        for (String s : entities.keySet()) {

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureCache.getAtlas(s));
            glUniform1i(textureCache.getAtlas(s), 0);

            for (Entity e : entities.get(s)) {

                if (e.pos.distance(cameraLocation) < WIDTH) {

                    shaderProgram.setUniform("projection", calculateProjection(e.pos));

                    shaderProgram.setUniform("frame", e.attackable ? getEntityTextureTranslation(e.frame) : getNonAttackableEntityTextureTranslation(e.frame));

                    if (!e.glowing)
                        shaderProgram.setUniform("hit", new Vector4f(2 - e.getHitMod(), 1, 1, e.getHitMod()));
                    else
                        shaderProgram.setUniform("hit", e.getGlow());

                    e.render();
                    rendercount++;

                } else
                    e.tickAnim();
            }
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, player.texId);
        glUniform1i(player.texId, 0);

        shaderProgram.setUniform("projection", calculateProjection(player.pos));

        shaderProgram.setUniform("frame", getEntityTextureTranslation(player.frame));

        shaderProgram.setUniform("hit", new Vector4f(2 - player.getHitMod(), 1, 1, player.getHitMod()));

        player.render();

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    Matrix4f scale_font = new Matrix4f().scale(1.0f / 16.0f, 1.0f / 16.0f, 0);

    private Matrix4f getFontTextureTranslation(byte c) {
        return scale_font.translate(c % 16, c / 16, 0, new Matrix4f());
    }

    private void setAction(String n, String s) {
        if (text_action != null) {
            text_action.text = "[Press ENTER] " + s + " : " + n;
            showAction = true;
            return;
        }
        text_action = new TextObject("[Press ENTER] " + s + " : " + n, new Vector3f((-WIDTH / 2) + 10, -(HEIGHT / 2) + 16, 0));
        showAction = true;
    }

    private void drawchat() {
        if (showChat) {
            renderText(chat);
        }
    }

    private void drawAction() {
        if (showAction) {
            renderStaticText(text_action);
            return;
        }
    }

    private void drawfps() {
        if (text_fps != null) {
            text_fps.text = "fps: " + fps;
            renderStaticText(text_fps);
            return;
        }
        text_fps = new TextObject("fps: " + fps, new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16, 0));
        renderStaticText(text_fps);
    }

    private void drawmaxlevel() {
        if (text_max != null) {
            renderStaticText(text_max);
            return;
        }
        text_max = new TextObject("Current max level: " + currmaxlevel, new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 8, 0));
        renderStaticText(text_fps);
    }

    private void drawstats() {
        if (text_stats != null) {
            text_stats.text = "level: " + player.level + " xp: [" + (int) player.xp + "/" + (int) Entity.getExpForLevel(player.level + 1) + "]";
            renderStaticText(text_stats);
            return;
        }
        text_stats = new TextObject("level: " + player.level + " xp: [" + (int) player.xp + "/" + (int) Entity.getExpForLevel(player.level + 1) + "]", new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 7, 0));
        renderStaticText(text_stats);
    }

    private void drawloc() {
        if (text_loc != null) {
            text_loc.text = "loc: " + (int) player.pos.x + ", " + (int) player.pos.y + " [tile " + (int) player.pos.x / 64 + ", " + (int) (player.pos.y / 64) + "]";
            renderStaticText(text_loc);
            return;
        }
        text_loc = new TextObject("loc: " + (int) player.pos.x + "," + (int) player.pos.y + " [" + (int) player.pos.x / 64 + "," + (int) (player.pos.y / 64) + "]", new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 2, 0));
        renderStaticText(text_loc);
    }

    private void drawcount() {
        if (text_count != null) {
            text_count.text = "entity count: " + rendercount + " / " + rendertotal;
            renderStaticText(text_count);
            return;
        }
        text_count = new TextObject("entity count: " + rendercount + " / " + rendertotal, new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 3, 0));
        renderStaticText(text_count);
    }

    private void drawhp() {
        if (text_hp != null) {
            text_hp.text = "hp: " + (int) player.hitpoints + " / " + (int) player.max_hitpoints;
            renderStaticText(text_hp);
            return;
        }
        text_hp = new TextObject("hp: " + (int) player.hitpoints + " / " + (int) player.max_hitpoints, new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 6, 0));
        renderStaticText(text_hp);
    }

    private void drawtilecount() {
        if (tile_count != null) {
            tile_count.text = "tile count: " + tilecount + " / " + tiletotal;
            renderStaticText(tile_count);
            return;
        }
        tile_count = new TextObject("tile count: " + tilecount + " / " + tiletotal, new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 4, 0));
        renderStaticText(tile_count);
    }

    private void drawprojcount() {
        if (proj_count != null) {
            proj_count.text = "projectile count: " + projectilecount + " / " + projectiletotal;
            renderStaticText(proj_count);
            return;
        }
        proj_count = new TextObject("projectile count: " + projectilecount + " / " + projectiletotal, new Vector3f((-WIDTH / 2) + 10, (HEIGHT / 2) - 16 * 5, 0));
        renderStaticText(proj_count);
    }

    private void drawTextObs() {
        for (TextObject t : textObjects) {
            if (!t.destroy)
                renderText(t);
            else
                dest_texts.add(t);
        }
        for (TextObject t : dest_texts) {
            textObjects.remove(t);
        }
        dest_texts.clear();
    }

    private void drawHitTexts() {
        for (HitTextObject t : hitTexts) {
            if (!t.destroy)
                renderHitText(t);
            else
                dest_hittexts.add(t);
        }
        for (HitTextObject t : dest_hittexts) {
            hitTexts.remove(t);
        }
        dest_hittexts.clear();
    }

    private void drawOverheadText() {

        for (String s : entities.keySet()) {
            for (Entity entity : entities.get(s)) {
                shaderProgram.setUniform("hit", entity.attackable ? new Vector4f(1 - (entity.hitpoints / entity.max_hitpoints), (entity.hitpoints / entity.max_hitpoints), 0, 1) : new Vector4f(1, 1, 1, 1));
                renderText(entity.overhead);
                if (entity.attackable)
                    renderText(new TextObject("LVL " + entity.level, new Vector3f(entity.pos.x, entity.pos.y - 66, 0)));
            }
        }

        shaderProgram.setUniform("hit", new Vector4f(1 - (player.hitpoints / player.max_hitpoints), 0, 2f * (player.hitpoints / player.max_hitpoints), 1));
        renderText(player.overhead);
        renderText(new TextObject("LVL " + player.level, new Vector3f(player.pos.x, player.pos.y - 66, 0)));
    }

    private void renderText(TextObject t) {

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, TextObject.FONT_TEX);
        glUniform1i(TextObject.FONT_TEX, 0);

        int offset = 0;

        for (byte c : t.getBytes()) {

            shaderProgram.setUniform("projection", calculateProjection(t.pos).translate(offset++ * 10 - ((t.text.length() - 2) * 6), 32, 0));

            shaderProgram.setUniform("frame", getFontTextureTranslation(c));

            t.render();

        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void renderHitText(HitTextObject t) {

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, TextObject.FONT_TEX);
        glUniform1i(TextObject.FONT_TEX, 0);

        int offset = 0;

        Vector3f pos = t.getPos();

        shaderProgram.setUniform("hit", new Vector4f(2, 1, 1, t.getHitMod()));

        for (byte c : t.getBytes()) {

            shaderProgram.setUniform("projection", calculateProjection(pos).translate(offset++ * 10 - ((t.text.length() - 2) * 6), 0, 0));

            shaderProgram.setUniform("frame", getFontTextureTranslation(c));

            t.render();
        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void renderStaticText(TextObject t) {

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, TextObject.FONT_TEX);
        glUniform1i(TextObject.FONT_TEX, 0);

        int offset = 0;

        for (byte c : t.getBytes()) {

            shaderProgram.setUniform("projection", new Matrix4f(projection).translate(t.pos).translate(offset++ * 10, 0, 0));

            shaderProgram.setUniform("frame", getFontTextureTranslation(c));

            t.render();

        }

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    Matrix4f scale_map = new Matrix4f().scale(1.0f / 16.0f, 1.0f / 235.0f, 0);

    private Matrix4f getMapTranslation(int id) {
        return scale_map.translate(id % 16, id / 16, 0, new Matrix4f());
    }

    private void renderWorld() {

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, map.tex_id);
        glUniform1i(map.tex_id, 0);

        tilecount = 0;

        for (Tile t : map.prepareTiles(cameraLocation)) {

            shaderProgram.setUniform("projection", calculateProjection(t.pos));

            shaderProgram.setUniform("frame", getMapTranslation(t.id));

            t.render();

            tilecount++;
        }

        for (Tile t : map.getDecorators()) {

            if (Math.abs((t.pos.x / 64) - (cameraLocation.x / 64)) <= WIDTH / 128 + 2 && Math.abs((t.pos.y / 64) - (cameraLocation.y / 64)) <= HEIGHT / 128 + 2) {

                shaderProgram.setUniform("projection", calculateProjection(t.pos));

                shaderProgram.setUniform("frame", getMapTranslation(t.id));

                t.render();

                tilecount++;
            }
        }

        for (Tile t : map.getNonWalkables()) {

            if (Math.abs((t.pos.x / 64) - (cameraLocation.x / 64)) <= WIDTH / 128 + 2 && (Math.abs((t.pos.y / 64) - (cameraLocation.y / 64))) <= HEIGHT / 128 + 2) {

                shaderProgram.setUniform("projection", calculateProjection(t.pos));

                shaderProgram.setUniform("frame", getMapTranslation(t.id));

                t.render();

                tilecount++;
            }
        }

        for (Portal t : map.getPortals()) {

            if (Math.abs((t.pos.x / 64) - (cameraLocation.x / 64)) <= WIDTH / 128 + 2 && (Math.abs((t.pos.y / 64) - (cameraLocation.y / 64))) <= HEIGHT / 128 + 2) {

                shaderProgram.setUniform("projection", calculateProjection(t.pos));

                shaderProgram.setUniform("frame", getMapTranslation(t.id));

                if (!t.glowing)
                    shaderProgram.setUniform("hit", new Vector4f(1, 1, 1, 1));
                else
                    shaderProgram.setUniform("hit", t.getGlow());

                t.render();

                tilecount++;
            }
        }

        glBindTexture(GL_TEXTURE_2D, 0);

    }

    private void renderWorldTop() {

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, map.tex_id);
        glUniform1i(map.tex_id, 0);


        for (Tile t : map.getDecoratorsTop()) {

            if (Math.abs((t.pos.x / 64) - (cameraLocation.x / 64)) <= WIDTH / 128 + 2 && Math.abs((t.pos.y / 64) - (cameraLocation.y / 64)) <= HEIGHT / 128 + 2) {

                shaderProgram.setUniform("projection", calculateProjection(t.pos));

                shaderProgram.setUniform("frame", getMapTranslation(t.id));

                t.render();

                tilecount++;
            }
        }

        glBindTexture(GL_TEXTURE_2D, 0);

    }

    private void update() {

        double curr = System.nanoTime() / 1000000000.0;

        for (StaticAnim n : anims) {
            if (!n.destroy)
                n.update(curr);
            else
                dest_anims.add(n);
        }

        for (Projectile p : projectiles) {
            if (player.pos.distance(p.pos) <= 32) {
                float dmg = (float) (Math.random() * p.damage);

                Entity a = getEntityForId(p.eid);

                if (dmg > player.hitpoints) {
                    dmg = player.hitpoints;

                    if (a != null)
                        a.underAttack = false;
                }

                hitTexts.add(new HitTextObject(dmg, new Vector3f(player.pos)));
                player.hit(dmg, p.eid);

                if (a != null) {
                    a.addExp((dmg + (a.underAttack ? 0 : 15 * a.level)) / 60);
                }

                p.destroy = true;

            } else if (!map.checkWalkable(p.pos.x, p.pos.y)) {
                p.destroy = true;
            }

            if (!p.destroy)
                p.update();
            else
                dest_projectiles.add(p);
        }

        if (!player.teleporting)
            player.move(MOVE_SPEED * delta.x, MOVE_SPEED * delta.y);

        player.playerUpdate();
        checkWalkableTarget(player);

        if (player.attacking)
            attack(player);

        //    if (delta.x != 0 || delta.y != 0 || player.teleporting) {
        updateCamera();
        setProjection();
        //    }

        if (player.died) {
            while (!map.checkWalkable(player.pos.x, player.pos.y)) {
                player.setPosition((float) (Math.random() * (map.width - 1) * 64), (float) (Math.random() * (map.height - 1) * 64));
            }
            updateCamera();
            setProjection();
            player.died = false;
        }

        player.addExp(0.001f);

        for (Portal p : map.getPortals()) {
            if (p.pos.distance(player.pos) <= 96) {
                if (interactableid == -1 && portalid == -1) {
                    p.glowing = true;
                    portalid = p.pid;
                    setAction("Portal", "Go through");
                }
            } else {
                if (portalid == p.pid) {
                    portalid = -1;
                    interacting = false;
                    showChat = false;
                    showAction = false;
                }
                p.glowing = false;
            }
        }


        for (String s : entities.keySet()) {

            for (Entity e : entities.get(s)) {
                if (e.pos.distance(player.pos) <= WIDTH * 2) {
                    if (e.interactable) {
                        if (e.pos.distance(player.pos) <= 96) {
                            if (interactableid == -1 && portalid == -1) {
                                if (curr - actiontimer >= 0.5) {
                                    e.glowing = true;
                                    interactableid = e.eid;
                                    setAction(e.name, "Talk to");
                                }
                            } else if (interactableid == e.eid) {
                                if (!showAction) {
                                    if (curr - actiontimer >= 0.5) {
                                        setAction(e.name, "Talk to");
                                        showAction = true;
                                    }
                                }
                            }
                        } else {
                            if (interactableid == e.eid) {
                                interactableid = -1;
                                interacting = false;
                                showChat = false;
                                showAction = false;
                            }
                            e.canMove = true;
                            e.glowing = false;
                        }
                    } else if (e.attackable) {
                        for (Projectile p : projectiles) {
                            if (!p.destroy) {
                                if (e.pos.distance(p.pos) <= 31) {
                                    float dmg = (float) (Math.random() * p.damage);

                                    Entity a = getEntityForId(p.eid);

                                    if (dmg > e.hitpoints) {
                                        dmg = e.hitpoints;

                                        if (a != null)
                                            a.underAttack = false;
                                    }

                                    hitTexts.add(new HitTextObject(dmg, new Vector3f(e.pos)));
                                    e.hit(dmg, p.eid);
                                    p.destroy = true;

                                    if (a != null) {
                                        a.addExp((dmg + (a.underAttack ? 0 : 15 * a.level)) / 60);
                                    }
                                }
                            }
                        }

                        if (e.underAttack) {
                            Entity t = getEntityForId(e.attackerId);
                            if (t != null)
                                e.setTarget(t.pos);
                            else
                                e.underAttack = false;
                        }
                    }

                    e.update(curr);
                    checkWalkableTarget(e);

                    if (e.begin_cast) {
                        e.startCast(attackInfoCache.get(player.attack).cast);
                    }

                    if (e.died) {
                        while (!map.checkWalkable(e.pos.x, e.pos.y)) {
                            e.setPosition((float) (Math.random() * (map.width - 1) * 64), (float) (Math.random() * (map.height - 1) * 64));
                        }
                        e.died = false;
                    } else if (e.attacking)
                        attack(e);

                    else if (e.tele_anim) {
                        createStaticAnim("teleport", e.pos, true);
                        e.tele_anim = false;
                    }

                    e.addExp(0.01f);

                }
            }

            for (Projectile p : dest_projectiles) {
                projectiles.remove(p);
                projectiletotal--;
            }

            for (StaticAnim p : dest_anims) {
                anims.remove(p);
                //    animtotal--;
            }

            dest_projectiles.clear();
            dest_anims.clear();
        }
    }

    private Entity getEntityForId(int id) {
        if (id == player.eid)
            return player;

        for (String s : entities.keySet()) {
            for (Entity e : entities.get(s)) {
                if (e.eid == id)
                    return e;
            }
        }
        System.out.println("eror wot");
        return null;
    }

    private void checkWalkableTarget(Entity e) {

        float x = e.pos.x + e.movement.x;
        float y = e.pos.y + e.movement.y;

        if (x <= 0 || y >= (map.height - 1) * 64 || y <= 0 || x >= (map.width - 1) * 64)
            e.movement.zero();

        else if (!map.checkWalkable(x, y))
            e.movement.zero();

        if (!e.teleporting)
            e.updateMovement();

    }

    private void updateCamera() {
        if (player.pos.y < (HEIGHT / 2) - 32)
            cameraLocation.y = (HEIGHT / 2) - 32;
        else if (player.pos.y >= (map.height * 64) - (HEIGHT / 2) - 32)
            cameraLocation.y = (map.height * 64) - (HEIGHT / 2) - 32;
        else
            cameraLocation.y = player.pos.y;

        if (player.pos.x < (WIDTH / 2) - 32)
            cameraLocation.x = (WIDTH / 2) - 32;
        else if (player.pos.x >= (map.width * 64) - (WIDTH / 2) - 32)
            cameraLocation.x = (map.width * 64) - (WIDTH / 2) - 32;
        else
            cameraLocation.x = player.pos.x;
    }

    private void setProjection() {
        viewMatrix = new Matrix4f(projection).translate(new Vector3f(cameraLocation).negate()).mul(world);
    }

    private Matrix4f calculateProjection(Vector3f location) {
        return getProjection().translate(location);
    }

    private Matrix4f getProjection() {
        return new Matrix4f(viewMatrix);
    }

    private boolean isKeyPressed(int keyCode) {
        return glfwGetKey(window, keyCode) == GLFW_PRESS;
    }

    private void generateEntities() {

        Entity.init();

        entityInfoCache = new EntityInfoCache();

        for (EntityInfoCache.EntityInfo e : entityInfoCache.entities.values())
            textureCache.getAtlas(e.atlas);

        entities = new HashMap<>();

        EntityInfoCache.EntityInfo ei = entityInfoCache.get("Archer");

        player = new Entity(textureCache.getAtlas(ei.atlas), ei.speed, ei.attack, "blowmelol");
        player.setPosition((float) (Math.random() * (map.width - 1) * 64), (float) (Math.random() * (map.height - 1) * 64));

        while (!map.checkWalkable(player.pos.x, player.pos.y))
            player.setPosition((float) (Math.random() * (map.width - 1) * 64), (float) (Math.random() * (map.height - 1) * 64));

        player.range = attackInfoCache.get(ei.attack).range * 32;
        player.overhead = new TextObject("blowmelol", player.pos);

        Entity entity2;

        for (int i = 0; i <= 550; i++) {
            ei = entityInfoCache.getRandom();
            entity2 = new Entity(textureCache.getAtlas(ei.atlas), ei.speed, ei.attack, ei.name);

            do
                entity2.setPosition((float) (Math.random() * (map.width - 1) * 64), (float) (Math.random() * (map.height - 1) * 64));
            while (!map.checkWalkable(entity2.pos.x, entity2.pos.y));

            entity2.setAnimDir((int) (Math.random() * 4));

            if (entity2.attackable)
                entity2.range = attackInfoCache.get(ei.attack).range * 32;

            if (ei.interactable)
                entity2.interactable = true;

            if (!ei.dialoguedir.equals("none")) {
                entity2.interactable = true;
                entity2.dialoguedir = ei.dialoguedir;
            }

            addEntity(ei.atlas, entity2);
            entity2.overhead = new TextObject(ei.name, entity2.pos);
        }

        updateCamera();
    }

    private void addEntity(String n, Entity e) {
        List l = entities.get(n);
        rendertotal++;

        if (l != null) {
            l.add(e);
            return;
        }

        l = new ArrayList<Entity>();
        l.add(e);
        entities.put(n, l);
    }

    private void createProjectile(int eid, String name, Vector3f loc, Vector2f dir, float dmgmod) {
        Vector3f origin = loc.add(new Vector3f(new Vector2f(dir).mul(33), 0));

        AttackInfoCache.AttackInfo ai = attackInfoCache.get(name);

        if (ai.directional)
            projectiles.add(new Arrow(eid, ai, origin, dir, dmgmod));
        else
            projectiles.add(new Projectile(eid, ai, origin, dir, dmgmod));

        projectiletotal++;

    }

    private void createStaticAnim(String name, Vector3f loc, boolean loop) {

        AnimInfoCache.AnimInfo ai = animInfoCache.get(name);

        anims.add(new StaticAnim(ai, loc, loop));

        //  animtotal++;

    }

    private Matrix4f getProjectileTranslation(int frame, int frames) {
        Matrix4f scale = new Matrix4f().scale(1.0f / frames, 1.0f, 0);
        return scale.translate(frame, 0, 0);
    }

    private void renderProjectiles() {
        projectilecount = 0;

        for (Projectile e : projectiles) {

            if (e.destroy) {
                continue;
            }

            if (e.pos.distance(cameraLocation) < WIDTH) {

                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, textureCache.getAtlas(e.atlas));
                glUniform1i(textureCache.getAtlas(e.atlas), 0);

                shaderProgram.setUniform("projection", calculateProjection(e.pos));

                shaderProgram.setUniform("frame", getProjectileTranslation(e.frame, e.frames));

                e.render();

                projectilecount++;

                glBindTexture(GL_TEXTURE_2D, 0);
            } else
                e.tickAnim();
        }
    }

    private void renderAnims() {
        //animcount = 0;

        for (StaticAnim e : anims) {

            if (e.destroy) {
                continue;
            }

            if (e.pos.distance(cameraLocation) < WIDTH) {

                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, textureCache.getAtlas(e.atlas));
                glUniform1i(textureCache.getAtlas(e.atlas), 0);

                shaderProgram.setUniform("projection", calculateProjection(e.pos));

                shaderProgram.setUniform("frame", getProjectileTranslation(e.frame, e.frames));

                e.render();

                projectilecount++;

                glBindTexture(GL_TEXTURE_2D, 0);
            } else
                e.tickAnim();
        }
    }

}
