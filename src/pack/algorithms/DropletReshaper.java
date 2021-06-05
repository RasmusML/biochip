package pack.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.math.MathUtils;
import pack.helpers.GeometryUtil;

public class DropletReshaper {

  private boolean[][] targetShape;
  private int[][] currentShape; // ids
  
  private List<Point> shape;
  private Droplet droplet;
  
  private Comparator<Point> firstIterationSorter;
  private Comparator<Point> secondIterationSorter;
  
  private Map<Point, Point> pointToTarget;
  private Map<Point, Integer> pointToMinOutsidePointDistance;
  
  private MoveFinder moveFinder;
  private BioArray array;
  
  public DropletReshaper(MoveFinder moveFinder, BioArray array) {
    this.moveFinder = moveFinder;
    
    currentShape = new int[array.width][array.height];
    targetShape = new boolean[array.width][array.height];
    
    firstIterationSorter = (p1, p2) -> {
      // @report if both droplets are within the target shape, select the droplet closest to a droplet outside the target shape, to move.
      // This prevents selecting a droplet within the target shape which does not get closer to the final target shape (filling all tiles of the target shape)
      if (targetShape[p1.x][p1.y] && targetShape[p2.x][p2.y]) {
        int d1 = pointToMinOutsidePointDistance.get(p1);
        int d2 = pointToMinOutsidePointDistance.get(p2);
        return d1 - d2;
      } else {
        int v1 = targetShape[p1.x][p1.y] ? 1 : -1;
        int v2 = targetShape[p2.x][p2.y] ? 1 : -1;
        return v1 - v2;
      }
    };
    
    secondIterationSorter = (p1, p2) -> {
      Point t1 = pointToTarget.get(p1);
      Point t2 = pointToTarget.get(p2);
      
      int d1 = (int) MathUtils.getManhattanDistance(p1.x, p1.y, t1.x, t1.y);
      int d2 = (int) MathUtils.getManhattanDistance(p2.x, p2.y, t2.x, t2.y);
      
      return d1 - d2;
    };
  }
  
  public void reshape(Droplet droplet, List<Point> shape) {
    this.droplet = droplet;
    this.shape = shape;
    
    for (int i = 0; i < droplet.units.size(); i++) {
      DropletUnit unit = droplet.units.get(i);
      Point at = unit.route.getPosition();
      
      int id = i + 1;
      currentShape[at.x][at.y] = id;
    }
    
    for (Point point : shape) {
      targetShape[point.x][point.y] = true;
    }
  }
  
  public void step(List<Droplet> droplets, List<Module> modules, int timestamp) {
    pointToTarget = new HashMap<>();
    pointToMinOutsidePointDistance = new HashMap<>();
    
    List<Point> left = new ArrayList<>();
    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition();

      int minDistance = Integer.MAX_VALUE;
      Point closestTarget = null;
      
      for (Point target : shape) {
        int distance = (int) MathUtils.getManhattanDistance(at.x, at.y, target.x, target.y);
        
        if (distance < minDistance) {
          minDistance = distance;
          closestTarget = target;
        }
      }
      
      pointToTarget.put(at, closestTarget);
      
      left.add(at);
    }

    List<Point> inside = new ArrayList<>();
    List<Point> outside = new ArrayList<>();
    
    // @report see dropletResizeIssue for why inside/outside is necessary.
    for (Point droplet : left) {
      if (targetShape[droplet.x][droplet.y]) {
        inside.add(droplet);
      } else {
        outside.add(droplet);
      }
    }
    
    boolean anyMove = true;
    while (anyMove) {
      
      pointToMinOutsidePointDistance.clear();

      for (Point insider : inside) {
        int minDistance = Integer.MAX_VALUE;
        for (Point outsider : outside) {
          int distance = (int) MathUtils.getManhattanDistance(insider.x, insider.y, outsider.x, outsider.y);
          
          if (distance < minDistance) {
            minDistance = distance;
          }
        }
        
        pointToMinOutsidePointDistance.put(insider, minDistance);
      }
      
      left.sort(firstIterationSorter);

      anyMove = false;
      for (Point droplet : left) {
        Move move = getFillingMove(droplet, droplets, modules, timestamp);
        
        if (move != null) {
          left.remove(droplet);
          updateInternalPosition(droplet, move);
          
          if (targetShape[droplet.x][droplet.y]) {
            inside.add(droplet);
            outside.remove(droplet);
          }

          anyMove = true;
          
          break;
        }
      }
    }
    
    // points not moved are either not inside the target shape or they "stuck" within the shape.
    // the points not within the target shape move those closer.
    
    left.sort(secondIterationSorter);

    for (Point droplet : left) {
      if (targetShape[droplet.x][droplet.y]) continue;
      
      Move move = getNonFillingMove(droplet, droplets, modules, timestamp);
      
      updateInternalPosition(droplet, move);
    }
  }

  private void updateInternalPosition(Point candidate, Move move) {
    int id = currentShape[candidate.x][candidate.y];
    currentShape[candidate.x][candidate.y] = 0;
    
    candidate.add(move.x, move.y);
    currentShape[candidate.x][candidate.y] = id;
  }

  private Move getFillingMove(Point at, List<Droplet> droplets, List<Module> modules, int timestamp) {
    Point target = new Point();
    
    List<Move> moves = moveFinder.getValidMoves(droplet, timestamp, droplets, modules, array);
    for (Move move : moves) {
      if (move == Move.None) continue;
      
      target.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(target.x, target.y, 0, 0, array.width, array.height)) continue;
      
      if (targetShape[target.x][target.y] && currentShape[target.x][target.y] == 0) {
        return move;
      }
    }
    
    return null;
  }

  private Move getNonFillingMove(Point at, List<Droplet> droplets, List<Module> modules, int timestamp) {
    Point target = pointToTarget.get(at);
    Point to = new Point();
    
    int minDistance = Integer.MAX_VALUE;
    Move selected = null;
    
    List<Move> moves = moveFinder.getValidMoves(droplet, timestamp, droplets, modules, array);
    for (Move move : moves) {
      to.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, 0, 0, array.width, array.height)) continue;
      if (move != Move.None && currentShape[to.x][to.y] != 0) continue;
      
      int distance = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);
      
      if (distance < minDistance) {
        minDistance = distance;
        selected = move;
      }
    }
    
    return selected;
  }
}
