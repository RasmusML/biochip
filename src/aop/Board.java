package aop;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Point;

public class Board {

  public int[][] grid;
  
  public List<Point> openTiles;
  
  public Board(String layout) {
    grid = buildLayout(layout);
    
    openTiles = getOpenTiles();
  }
  
  private int[][] buildLayout(String layout) {
    String[] rows = layout.split("\n");
    
    int height = rows.length;
    int width = rows[0].length();
    
    int[][] build = new int[width][height];
    
    for (int y = 0; y < height; y++) {
      int yy = height - 1 - y;

      String rowRaw = rows[y];
      String[] row = rowRaw.split("");
      
      for (int x = 0; x < width; x++) {
        int value = Integer.parseInt(row[x]);
        build[x][yy] = value;
      }
    }
    
    return build;
  }

  public int getWidth() {
    return grid.length;
  }
  
  public int getHeight() {
    return grid[0].length;
  }
  
  private List<Point> getOpenTiles() {
    List<Point> tiles = new ArrayList<>();
    
    for (int x = 0; x < getWidth(); x++) {
      for (int y = 0; y < getHeight(); y++) {
        if (isTileOpen(x, y)) tiles.add(new Point(x, y));
      }
    }
    
    return tiles;
  }

  public boolean isTileOpen(int tx, int ty) {
    if (tx < 0 || tx >= getWidth()|| ty < 0 || ty >= getHeight()) return false;
    return grid[tx][ty] == 1;
  }
}
