package com.km.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class EntityInfoCache {

    public HashMap<String, EntityInfo> entities;
    public HashMap<String, ArrayList<String>> questinfo;

    public EntityInfoCache() {
        entities = new HashMap<>();
        questinfo = new HashMap<>();
        generateEntityInfo();
    }

    private void generateEntityInfo() {

        String input;
        String[] tokens;
        String name = "";
        int id = 0;
        float speed = 0f;
        String atlas = "";
        String attack = "";
        String dialoguedir = "none";
        boolean attackable = true;
        boolean interactable = false;

        try {
            File dir = new File("./res/entityinfo/");

            BufferedReader br;

            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".en")) {
                    br = new BufferedReader(new FileReader(f));

                    attackable = true;
                    interactable = false;
                    dialoguedir = "none";

                    while (!(input = br.readLine()).contains("EOF")) {

                        tokens = input.split("=");

                        if (tokens[0].equals("NAME")) {
                            name = tokens[1];
                        } else if (tokens[0].equals("EID")) {
                            id = Integer.valueOf(tokens[1]);
                        } else if (tokens[0].equals("SPEED")) {
                            speed = Float.valueOf(tokens[1]);
                        } else if (tokens[0].equals("ATLAS")) {
                            atlas = tokens[1];
                        } else if (tokens[0].equals("INTERACTABLE")) {
                            interactable = tokens[1].equals("true");
                        } else if (tokens[0].equals("DIALOGUE")) {

                            if (!interactable)
                                interactable = true;

                            dialoguedir = tokens[1];

                            if (!questinfo.containsKey(dialoguedir)) {

                                BufferedReader br2 = new BufferedReader(new FileReader(new File("./res/chat/" + dialoguedir + ".ed")));
                                String d;
                                ArrayList<String> dialogue = new ArrayList<>();
                                while (!(d = br2.readLine()).equals("EOF")) {
                                    dialogue.add(d);
                                }
                                if (questinfo != null)
                                    questinfo.put(dialoguedir, dialogue);
                                else {
                                    questinfo = new HashMap<>();
                                    questinfo.put(dialoguedir, dialogue);
                                }
                            }
                        } else if (tokens[0].equals("ATTACK")) {
                            attack = tokens[1];
                            if (attack.equals("none")) {
                                attackable = false;
                            }
                        }
                    }

                    entities.put(name, new EntityInfo(name, id, speed, atlas, attack, attackable, interactable, dialoguedir));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed @ last: " + name + "  [ID=" + id + "] [ATLAS=" + atlas + "] [ATTACK=" + attack + "]");
            e.printStackTrace();
        }
    }

    int chatoffset = 0;
    public String getRandomQuest(String name) {

        ArrayList<String> inf = questinfo.get(name);
        if (inf != null) {

            int chatoffset = (int)(Math.random() * inf.size());
            return inf.get(chatoffset);
        }

        return "noll";
    }

    public String getQuest(String name, int offset) {

        ArrayList<String> inf = questinfo.get(name);
        if (inf != null) {
            return inf.get(offset);
        } else {
            try {
                BufferedReader br2 = new BufferedReader(new FileReader(new File("./res/chat/" + name + ".ed")));
                String d;
                ArrayList<String> dialogue = new ArrayList<>();
                while (!(d = br2.readLine()).equals("EOF")) {
                    dialogue.add(d);
                }
                if (questinfo != null)
                    questinfo.put(name, dialogue);
                else {
                    questinfo = new HashMap<>();
                    questinfo.put(name, dialogue);
                }
                return questinfo.get(name).get(offset);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "none";
    }

    public EntityInfo get(String name) {
        return entities.get(name);
    }

    public EntityInfo get(int id) {
        for (EntityInfo i : entities.values()) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    public EntityInfo getRandom() {
        return get((int)(Math.random() * entities.size()));
    }

    public static class EntityInfo {

        public String name;
        public int id;
        public String atlas;
        public String attack;
        public float speed;
        public boolean attackable;
        public boolean interactable;
        public String dialoguedir;

        public EntityInfo(String name, int id, float speed, String atlas, String attack, boolean attackable, boolean interactable, String dialoguedir) {
            this.name = name;
            this.id = id;
            this.atlas = atlas;
            this.attack = attack;
            this.speed = speed;
            this.attackable = attackable;
            this.interactable = interactable;
            this.dialoguedir = dialoguedir;
            System.out.println("Loaded entity info: " + name + "\t\t[ID=" + id + "]\t[ATLAS=" + atlas + "]\t[ATTACK=" + attack + "]");
        }

    }
}
