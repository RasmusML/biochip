package pack.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import engine.math.MathUtils;
import pack.algorithms.components.DisposableUidGenerator;
import pack.helpers.GeometryUtil;

public class DropletReshaper {

  private int[][] targetShape;
  
  private Comparator<DropletUnit> firstIterationSorter;
  private Comparator<DropletUnit> secondIterationSorter;
  
  private DisposableUidGenerator uidGenerator;
  
  private List<DropletReshapeTask> tasks;
  private DropletReshapeTask task;
  
  private Map<DropletUnit, Point> pointToTarget;
  private Map<DropletUnit, Integer> pointToMinOutsidePointDistance;
  
  private MoveFinder moveFinder;
  private BioArray array;
  
  public DropletReshaper(MoveFinder moveFinder, BioArray array) {
    this.moveFinder = moveFinder;
    
    targetShape = new int[array.width][array.height];

    tasks = new ArrayList<>();
    uidGenerator = new DisposableUidGenerator();

    firstIterationSorter = (p1, p2) -> {
      Point at1 = p1.route.getPosition();
      Point at2 = p2.route.getPosition();
      
      // @report if both droplets are within the target shape, select the droplet closest to a droplet outside the target shape, to move.
      // This prevents selecting a droplet within the target shape which does not get closer to the final target shape (filling all tiles of the target shape)
      if (targetShape[at1.x][at1.y] == task.id && targetShape[at2.x][at2.y] == task.id) {
        int d1 = pointToMinOutsidePointDistance.get(p1);
        int d2 = pointToMinOutsidePointDistance.get(p2);
        return d1 - d2;
      } else {
        int v1 = (targetShape[at1.x][at1.y] == task.id) ? 1 : -1;
        int v2 = (targetShape[at2.x][at2.y] == task.id) ? 1 : -1;
        return v1 - v2;
      }
    };
    
    secondIterationSorter = (p1, p2) -> {
      Point at1 = p1.route.getPosition();
      Point at2 = p2.route.getPosition();
      
      Point t1 = pointToTarget.get(p1);
      Point t2 = pointToTarget.get(p2);
      
      int d1 = (int) MathUtils.getManhattanDistance(at1.x, at1.y, t1.x, t1.y);
      int d2 = (int) MathUtils.getManhattanDistance(at2.x, at2.y, t2.x, t2.y);
      
      return d1 - d2;
    };
  }
  
  public void reshape(Droplet droplet, List<Point> shape) {
    int id = uidGenerator.getId();

    DropletReshapeTask task = new DropletReshapeTask();
    task.droplet = droplet;
    task.shape = shape;
    task.id = id;
    
    tasks.add(task);
    
    for (Point point : shape) {
      targetShape[point.x][point.y] = id;
    }
  }
  
  public boolean step(Droplet droplet, List<Droplet> droplets, List<Module> modules, int timestamp) {
    this.task = getTask(droplet);
    
    pointToTarget = new HashMap<>();
    pointToMinOutsidePointDistance = new HashMap<>();
    
    // compute the distance between each droplet-unit and the closest target-shape cell.
    List<DropletUnit> left = new ArrayList<>();
    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition();

      int minDistance = Integer.MAX_VALUE;
      Point closestTarget = null;
      
      for (Point target : task.shape) {
        int distance = (int) MathUtils.getManhattanDistance(at.x, at.y, target.x, target.y);
        
        if (distance < minDistance) {
          minDistance = distance;
          closestTarget = target;
        }
      }
      
      pointToTarget.put(unit, closestTarget);
      left.add(unit);
    }

    List<DropletUnit> inside = new ArrayList<>();
    List<DropletUnit> outside = new ArrayList<>();
    
    // @report see dropletResizeIssue for why inside/outside is necessary.
    for (DropletUnit unit : left) {
      Point at = unit.route.getPosition();
      
      if (targetShape[at.x][at.y] == task.id) {
        inside.add(unit);
      } else {
        outside.add(unit);
      }
    }
    
    boolean anyMove = true;
    while (anyMove) {
      
      pointToMinOutsidePointDistance.clear();

      // For each droplet unit inside the target-shape, compute the distance to the closest droplet-unit outside the target-shape.
      // We use this information to decide which droplet gets to select its move first.
      // The droplet-unit within the target-shape with the smallest distance to a droplet-unit outside the target-shape will move first.
      // The droplet-unit outside the target-shape must be adjacent to this droplet-unit inside the target-shape.
      // Thus moving the droplet-unit within the target-shape will result in the droplet-unit outside the target-shape will move into the target-shape.
      for (DropletUnit insider : inside) {
        int minDistance = Integer.MAX_VALUE;
        Point insiderAt = insider.route.getPosition();
        
        for (DropletUnit outsider : outside) {
          Point outsiderAt = outsider.route.getPosition();
          
          int distance = (int) MathUtils.getManhattanDistance(insiderAt.x, insiderAt.y, outsiderAt.x, outsiderAt.y);
          if (distance < minDistance) {
            minDistance = distance;
          }
        }
        
        pointToMinOutsidePointDistance.put(insider, minDistance);
      }
      
      left.sort(firstIterationSorter);

      anyMove = false;
      for (DropletUnit unit : left) {
        Point at = unit.route.getPosition();
        Move move = getFillingMove(unit, droplet, droplets, modules, timestamp);
        
        if (move != null) {
          left.remove(unit);
          
          Point to = at.copy().add(move.x, move.y);
          unit.route.path.add(to);
          
          if (targetShape[to.x][to.y] == task.id) {
            inside.add(unit);
            outside.remove(unit);
          }

          anyMove = true;
          
          break;
        }
      }
    }
    
    // points not moved are either not inside the target shape or they "stuck" within the shape.
    // the points not within the target shape move those closer.
    
    left.sort(secondIterationSorter);

    for (DropletUnit unit : left) {
      Point at = unit.route.getPosition();
      
      if (targetShape[at.x][at.y] == task.id) continue;
      
      Move move = getNonFillingMove(unit, droplet, droplets, modules, timestamp);
      Point to = at.copy().add(move.x, move.y);
      unit.route.path.add(to);
    }
    
    boolean doneReshaping = doneReshaping();
    if (doneReshaping) {
      clearTaskInTargetShape();

      uidGenerator.dispose(task.id);
      tasks.remove(task);
    }
    
    return doneReshaping;
    
  }
  
  private void clearTaskInTargetShape() {
    for (Point at : task.shape) {
      targetShape[at.x][at.y] = 0;
    }
  }

  private boolean doneReshaping() {
    for (Point at : task.shape) {
      if (targetShape[at.x][at.y] != task.id) return false;
    }
    
    return true;
  }

  private DropletReshapeTask getTask(Droplet droplet) {
    // we could use a hashmap (hashtable) here, but it is a bit overkill as "tasks" contains < 100 elements in any given moment (something closer to 5) for our tests.
    for (DropletReshapeTask task : tasks) {
      if (task.droplet == droplet) return task;
    }
    
    throw new IllegalStateException("no task for droplet exists!");
  }

  private Move getFillingMove(DropletUnit unit, Droplet droplet, List<Droplet> droplets, List<Module> modules, int timestamp) {
    Point at = unit.route.getPosition();
    Point to = new Point();
    
    List<Move> moves = moveFinder.getValidMoves(unit, droplet, timestamp, droplets, modules, array);
    for (Move move : moves) {
      if (move == Move.None) continue;
      
      to.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, 0, 0, array.width, array.height)) continue;
      
      if (targetShape[to.x][to.y] == task.id) {
        return move;
      }
    }
    
    return null;
  }

  private Move getNonFillingMove(DropletUnit unit, Droplet droplet, List<Droplet> droplets, List<Module> modules, int timestamp) {
    Point target = pointToTarget.get(unit);

    Point at = unit.route.getPosition();
    Point to = new Point();
    
    int minDistance = Integer.MAX_VALUE;
    Move selected = null;
    
    List<Move> moves = moveFinder.getValidMoves(unit, droplet, timestamp, droplets, modules, array);
    for (Move move : moves) {
      to.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, 0, 0, array.width, array.height)) continue;
      
      int distance = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);
      
      if (distance < minDistance) {
        minDistance = distance;
        selected = move;
      }
    }
    
    return selected;
  }

  static private class DropletReshapeTask {
    public int id;
    public Droplet droplet;
    public List<Point> shape;
  }
}

