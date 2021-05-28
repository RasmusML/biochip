package pack.algorithms.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.math.MathUtils;
import pack.algorithms.Move;
import pack.algorithms.Point;
import pack.algorithms.ShapedDroplet;
import pack.helpers.GeometryUtil;

public class DropletReshapeSimulator {
  
  private int width, height;

  private boolean[][] targetShape;
  private int[][] currentShape; // ids
  
  private ShapedDroplet droplet;
  private List<Point> shape;
  
  private Comparator<Point> firstIterationSorter;
  private Comparator<Point> secondIterationSorter;
  
  private Map<Point, Point> pointToTarget;
  private Map<Point, Integer> pointToMinOutsidePointDistance;
  
  public DropletReshapeSimulator() {
    
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
  
  public void reshape(ShapedDroplet droplet, List<Point> shape) {
    this.droplet = droplet;
    this.shape = shape;
    
    width = 8;
    height = 8;
    
    currentShape = new int[width][height];
    targetShape = new boolean[width][height];
    
    for (int i = 0; i < droplet.points.size(); i++) {
      Point tile = droplet.points.get(i);
      int id = i + 1;
      
      currentShape[tile.x][tile.y] = id;
    }
    
    for (Point point : shape) {
      targetShape[point.x][point.y] = true;
    }
  }
  
  public void step() {
    pointToTarget = new HashMap<>();
    pointToMinOutsidePointDistance = new HashMap<>();
    
    for (Point point : droplet.points) {
      int minDistance = Integer.MAX_VALUE;
      Point closestTarget = null;
      
      for (Point target : shape) {
        int distance = (int) MathUtils.getManhattanDistance(point.x, point.y, target.x, target.y);
        
        if (distance < minDistance) {
          minDistance = distance;
          closestTarget = target;
        }
      }
      
      pointToTarget.put(point, closestTarget);
    }
    
    List<Point> left = new ArrayList<>(droplet.points);

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
        Move move = getFillingMove(droplet);
        
        if (move != null) {
          left.remove(droplet);
          updatePosition(droplet, move);
          
          if (targetShape[droplet.x][droplet.y]) {
            inside.add(droplet);
            outside.remove(droplet);
          }

          anyMove = true;
          
          break;
        }
      }
    }
    
    // points not moved are either no inside the target shape or they "stuck" within the shape.
    // the points not within the target shape, move those closer.
    
    left.sort(secondIterationSorter);

    for (Point droplet : left) {
      if (targetShape[droplet.x][droplet.y]) continue;
      
      Move move = getNonFillingMove(droplet);
      
      updatePosition(droplet, move);
    }
  }

  private void updatePosition(Point candidate, Move move) {
    int id = currentShape[candidate.x][candidate.y];
    currentShape[candidate.x][candidate.y] = 0;
    
    candidate.add(move.x, move.y);
    currentShape[candidate.x][candidate.y] = id;
  }

  private Move getFillingMove(Point point) {
    Point target = new Point();
    
    Move[] moves = {Move.Left, Move.Right, Move.Up, Move.Down};
    for (Move move : moves) {
      target.set(point).add(move.x, move.y);
      
      if (!GeometryUtil.inside(target.x, target.y, 0, 0, width, height)) continue;
      
      if (targetShape[target.x][target.y] && currentShape[target.x][target.y] == 0) {
        return move;
      }
    }
    
    return null;
  }

  private Move getNonFillingMove(Point point) {
    Point target = pointToTarget.get(point);
    Point to = new Point();
    
    int minDistance = Integer.MAX_VALUE;
    Move selected = null;
    
    Move[] moves = Move.values();
    for (Move move : moves) {
      to.set(point).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, 0, 0, width, height)) continue;
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

