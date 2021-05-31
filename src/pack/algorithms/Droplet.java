package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

public class Droplet {
  
  public int id;
  public float area;
  
  public List<DropletUnit> units;
  public Operation operation;
  
  public Droplet() {
    units = new ArrayList<>();
  }
  
  public boolean hasPosition(int timestamp) {
    DropletUnit unit = units.get(0);
    Point position = unit.route.getPosition(timestamp);
    return position != null;
  }
  // we assume that all DropletUnits start at the same time for now.
  // So just get the timestamp of the first dropletunit.
  public int getStartTimestamp() {
    DropletUnit unit = units.get(0);
    return unit.route.start;
  }
  
  // we assume that all DropletUnits end at the same time for now.
  // So just get the timestamp of the first dropletunit.
  public int getEndTimestamp() {
    DropletUnit unit = units.get(0);
    return unit.route.start + unit.route.path.size();
  }

  public Point getCenterPosition() {
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
    
    for (DropletUnit unit : units) {
      Point at = unit.route.getPosition();
      
      if (at.x < minX) minX = at.x;
      if (at.y < minY) minY = at.y;
      if (at.x > maxX) maxX = at.x;
      if (at.y > maxY) maxY = at.y;
    }

    int cx = (maxX-minX) / 2;
    int cy = (maxY-minY) / 2;
    
    return new Point(minX + cx, minY + cy);
  }
}
