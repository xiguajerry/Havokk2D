package com.km.world;

import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class MapLoader {

    public static WorldMap loadMap(String map, int atlas) {

        try {

            File mapdat = new File("./res/maps/" + map + ".map");

            String input;

            BufferedReader br;

            String[] ins;

            int tx = 32;
            int ty = 32;
            String name = "";

            br = new BufferedReader(new FileReader(mapdat));

            while (!(input = br.readLine()).contains("EOF")) {
                ins = input.split("=");

                if (ins[0].equals("name")) {
                    name = ins[1];
                } else if (ins[0].equals("sizex")) {
                    tx = Integer.parseInt(ins[1]);
                } else if (ins[0].equals("sizey")) {
                    ty = Integer.parseInt(ins[1]);
                }
            }

            System.out.println("Loading map " + name + " | size " + tx + ":" + ty);

            File dirbg = new File("./res/maps/" + map + "_background.csv");
            File dirnw = new File("./res/maps/" + map + "_nonwalkable.csv");
            File dirdec = new File("./res/maps/" + map + "_decorators.csv");
            File dirdect = new File("./res/maps/" + map + "_decoratorstop.csv");
            File dirport = new File("./res/maps/" + map + "_portals.csv");

            Tile.init();
            Portal.init();

            Tile[][] tiles = new Tile[tx][ty];
            ArrayList<Tile> decs = new ArrayList<>();
            ArrayList<Tile> decst = new ArrayList<>();
            ArrayList<Tile> nws = new ArrayList<>();
            ArrayList<Portal> portals = new ArrayList<>();

            WorldMap m = new WorldMap(tx, ty, atlas);

            int height = 1;

            String[][] tokens = new String[ty][tx];

            br = new BufferedReader(new FileReader(dirbg));

            while (!(input = br.readLine()).contains("EOF")) {

                tokens[ty - height++] = input.split(",");

            }

            for (int i = 0; i < ty; i++) {
                for (int j = 0; j < tx; j++) {
                    tiles[j][i] = new Tile(new Vector3f((float) j * 64f, (float) i * 64f, 0f), Integer.parseInt(tokens[i][j]));
                }
            }
            m.setTiles(tiles);

            tokens = new String[ty][tx];

            br = new BufferedReader(new FileReader(dirdec));

            height = 1;

            while (!(input = br.readLine()).contains("EOF")) {

                tokens[ty - height++] = input.split(",");

            }

            for (int i = 0; i < ty; i++) {
                for (int j = 0; j < tx; j++) {
                    int id = Integer.parseInt(tokens[i][j]);
                    if (id != -1) {
                        decs.add(new Tile(new Vector3f((float) j * 64f, (float) i * 64f, 0f), id));
                    }
                }
            }
            m.setDecorators(decs);

            tokens = new String[ty][tx];

            br = new BufferedReader(new FileReader(dirdect));

            height = 1;

            while (!(input = br.readLine()).contains("EOF")) {

                tokens[ty - height++] = input.split(",");

            }

            for (int i = 0; i < ty; i++) {
                for (int j = 0; j < tx; j++) {
                    int id = Integer.parseInt(tokens[i][j]);
                    if (id != -1) {
                        decst.add(new Tile(new Vector3f((float) j * 64f, (float) i * 64f, 0f), id));
                    }
                }
            }
            m.setTopDecorators(decst);

            tokens = new String[ty][tx];

            br = new BufferedReader(new FileReader(dirnw));

            height = 1;

            while (!(input = br.readLine()).contains("EOF")) {

                tokens[ty - height++] = input.split(",");

            }

            for (int i = 0; i < ty; i++) {
                for (int j = 0; j < tx; j++) {
                    int id = Integer.parseInt(tokens[i][j]);
                    if (id != -1) {
                        nws.add(new Tile(new Vector3f((float) j * 64f, (float) i * 64f, 0f), id, false));
                    }
                }
            }
            m.setNonWalkables(nws);

            tokens = new String[ty][tx];

            br = new BufferedReader(new FileReader(dirport));

            height = 1;

            while (!(input = br.readLine()).contains("EOF")) {

                tokens[ty - height++] = input.split(",");

            }

            for (int i = 0; i < ty; i++) {
                for (int j = 0; j < tx; j++) {
                    int id = Integer.parseInt(tokens[i][j]);
                    if (id != -1) {
                        portals.add(new Portal(new Vector3f((float) j * 64f, (float) i * 64f, 0f), id));
                    }
                }
            }
            m.setPortals(portals);

            return m;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
