package com.km.gfx;

import de.matthiasmann.twl.utils.PNGDecoder;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengles.EXTSparseTexture.GL_TEXTURE_2D;

public class TextureCache {

    private HashMap<String, Texture> textures;

    public TextureCache() {
        textures = new HashMap<>();
    }

    public int getAtlas(String name) {
        Texture t = textures.get(name);

        if (t != null)
            return t.id;

        return (t = loadAtlas(name)) != null ? t.id : 0;
    }

    private Texture loadAtlas(String name) {

        if (textures.containsKey(name))
            return textures.get(name);

        try {

            PNGDecoder decoder = new PNGDecoder(new FileInputStream("res/textures/" + name + ".png"));

            ByteBuffer buf = ByteBuffer.allocateDirect(
                    4 * decoder.getWidth() * decoder.getHeight());
            decoder.decode(buf, decoder.getWidth() * 4, PNGDecoder.Format.RGBA);
            buf.flip();

            Texture t = new Texture(glGenTextures());
            // Bind the texture
            glBindTexture(GL_TEXTURE_2D, t.id);

            // Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte size
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            // Upload the texture data
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, decoder.getWidth(), decoder.getHeight(), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, buf);
            // Generate Mip Map
            glGenerateMipmap(GL_TEXTURE_2D);

            textures.put(name, t);

            System.out.println("Loaded atlas: " + name);

            return t;

        } catch (Exception e) {
            System.out.println("Error loading atlas: " + name);
            e.printStackTrace();
        }

        return null;
    }

}

class Texture {

    int id;

    public Texture(int id) {
        this.id = id;
    }

}