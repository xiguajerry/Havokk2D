package com.km.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class AnimInfoCache {
    public HashMap<String, AnimInfo> anims;

    public AnimInfoCache() {
        anims = new HashMap<>();
        generateAnimInfo();
    }

    private void generateAnimInfo() {

        String input;
        String[] tokens;
        String name = "";
        int id = 0;
        float lifetime = 0f;
        String atlas = "";
        int frames = 0;

        try {
            File dir = new File("./res/animinfo/");

            BufferedReader br;

            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".nn")) {
                    br = new BufferedReader(new FileReader(f));
                    while (!(input = br.readLine()).contains("EOF")) {

                        tokens = input.split("=");


                        if (tokens[0].equals("NAME")) {
                            name = tokens[1];
                        }
                        else if (tokens[0].equals("ID")) {
                            id = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("LIFETIME")) {
                            lifetime = Float.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("ATLAS")) {
                            atlas = tokens[1];
                        }
                        else if (tokens[0].equals("FRAMES")) {
                            frames = Integer.valueOf(tokens[1]);
                        }
                    }
                    anims.put(name, new AnimInfo(name, id, atlas, lifetime, frames));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed @ last: " + name + "  [ID=" + id + "] [ATLAS=" + atlas + "]");
            e.printStackTrace();
        }
    }

    public AnimInfo get(String name) {
        return anims.get(name);
    }

    public AnimInfo get(int id) {
        for (AnimInfo i : anims.values()) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    public static class AnimInfo {

        public final float lifetime;
        public final int frames;
        public String name;
        public int id;
        public String atlas;

        public AnimInfo(String name, int id, String atlas, float lifetime, int frames) {
            this.name = name;
            this.id = id;
            this.atlas = atlas;
            this.lifetime = lifetime;
            this.frames = frames;
            System.out.println("Loaded anim info: " + name + "\t\t[ID=" + id + "]\t[ATLAS=" + atlas + "]");
        }

    }
}

