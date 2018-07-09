package com.km.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class AttackInfoCache {

    public HashMap<String, AttackInfo> attacks;

    public AttackInfoCache() {
        attacks = new HashMap<>();
        generateAttackInfo();
    }

    private void generateAttackInfo() {

        String input;
        String[] tokens;
        String name = "";
        int id = 0;
        String atlas = "";
        float speed = 0;
        int range = 0;
        int frames = 0;
        int damage = 0;
        int cast = 0;
        int cooldown = 0;
        boolean directional = false;

        try {
            File dir = new File("./res/attackinfo/");

            BufferedReader br;

            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".an")) {
                    br = new BufferedReader(new FileReader(f));

                    directional = false;

                    while (!(input = br.readLine()).contains("EOF")) {

                        tokens = input.split("=");


                        if (tokens[0].equals("NAME")) {
                            name = tokens[1];
                        }
                        else if (tokens[0].equals("ID")) {
                            id = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("ATLAS")) {
                            atlas = tokens[1];
                        }
                        else if (tokens[0].equals("SPEED")) {
                            speed = Float.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("RANGE")) {
                            range = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("FRAMES")) {
                            frames = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("DAMAGE")) {
                            damage = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("CAST")) {
                            cast = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("COOLDOWN")) {
                            cooldown = Integer.valueOf(tokens[1]);
                        }
                        else if (tokens[0].equals("DIRECTIONAL")) {
                            directional = (Integer.valueOf(tokens[1]) == 1);
                        }
                    }
                    attacks.put(name, new AttackInfo(name, id, atlas, frames, speed, range, damage, cast, cooldown, directional));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed @ last: " + name + " [ID=" + id + "] [ATLAS=" + atlas + "] [SPEED=" + speed + "] [RANGE=" + range + "] [COOLDOWN=" + cooldown + "]");
            e.printStackTrace();
        }
    }

    public AttackInfo get(String name) {
        return attacks.get(name);
    }

    public AttackInfo get(int id) {
        for (AttackInfo i : attacks.values()) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    public static class AttackInfo {

        public String name;
        public int id;
        public String atlas;
        public float speed;
        public int range;
        public int frames;
        public int cast;
        public int cooldown;
        public int damage;
        public boolean directional = false;

        public AttackInfo(String name, int id, String atlas, int frames, float speed, int range, int damage, int cast, int cooldown, boolean directional) {
            this.name = name;
            this.id = id;
            this.atlas = atlas;
            this.frames = frames;
            this.speed = speed;
            this.range = range;
            this.damage = damage;
            this.cast = cast;
            this.cooldown = cooldown;
            this.directional = directional;
            System.out.println("Loaded attack info: " + name + "\t\t[ID=" + id + "]\t[ATLAS=" + atlas + "]\t[SPEED=" + speed + "] [RANGE=" + range + "] [DIRECTIONAL=" + directional  + "] [COOLDOWN=" + cooldown + "]");
        }

    }
}
