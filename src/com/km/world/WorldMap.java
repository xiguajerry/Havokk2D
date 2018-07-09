package com.km.world;

import com.km.Havokk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class WorldMap {

    Tile[][] tiles;
    public int tex_id;

    public int width, height;

    private static int TILESX;
    private static int TILESY;

    private ArrayList<Tile> nonWalkables;
    private ArrayList<Tile> decorators;
    private ArrayList<Tile> decorators_top;
    private ArrayList<Portal> portals;

    public Tile tileAt(int x, int y) {
        if (x < 0 || y < 0)
            return null;

        return tiles[x / 64][y / 64];
    }

    public Tile tileAt(float x, float y) {
        if (x < 0 || y < 0)
            return null;

        return tiles[((int)x >> 6)][((int)y >> 6)];
    }

    public WorldMap(int x, int y, int atlas) {

        width = x;
        height = y;

        tex_id = atlas;

        TILESX = (Havokk.WIDTH / 128) + 2;
        TILESY = (Havokk.HEIGHT / 128) + 1;
    }

    public List<Tile> prepareTiles(Vector3f loc) {

        List<Tile> t = new ArrayList<>();

        for (int i = -TILESX; i <= TILESX; i++) {
            if ((loc.x / 64) + i >= 0 && (loc.x / 64) + i < width) {

                for (int j = -TILESY; j <= TILESY; j++) {
                    if ((loc.y / 64) + j >= 0 && (loc.y / 64) + j < height) {
                        Tile tile = tiles[(int) (loc.x / 64) + i][(int) (loc.y / 64) + j];
                        if (tile != null)
                            t.add(tile);
                    }
                }
            }
        }
        return t;
    }

    public void setTiles(Tile[][] t) {
        tiles = t;
    }

    public void setNonWalkables(ArrayList<Tile> e) {
        nonWalkables = e;
    }

    public void setDecorators(ArrayList<Tile> e) {
        decorators = e;
    }

    public void setTopDecorators(ArrayList<Tile> e) {
        decorators_top = e;
    }

    public void setPortals(ArrayList<Portal> e) {
        portals = e;
    }

    public ArrayList<Tile> getDecorators() {
        return decorators;
    }

    public ArrayList<Tile> getDecoratorsTop() {
        return decorators_top;
    }

    public ArrayList<Tile> getNonWalkables() {
        return nonWalkables;
    }

    public ArrayList<Portal> getPortals() {
        return portals;
    }

    public boolean checkWalkable(float x, float y) {
        if (x < 0 || y < 0 || x > width * 64 || y > height * 64)
            return false;

        for (Tile t : nonWalkables) {
            if (Math.abs(t.pos.x - x) < 48) {
                if (y > t.pos.y) {
                    if (Math.abs(t.pos.y - y) < 64) {
                        return false;
                    }
                } else if (Math.abs(t.pos.y - y) < 36) {
                    return false;
                }
            }
        }
        return true;
    }

}
