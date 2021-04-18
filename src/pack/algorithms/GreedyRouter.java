package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GreedyRouter {

  private List<Operation> stalledOperations;
  private List<Operation> activatedOperations;
  private List<Operation> runningOperations;
  
  private int aliveOperationsCount;

  private List<Droplet> runningDroplets;
  private List<Droplet> retiredDroplets;

  private List<Reservior> reserviors;

  private Map<Integer, OperationExtra> operationIdToExtra;
  
  private UidGenerator dropletIdGenerator;

  int timestamp;

  public List<Route> compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    dropletIdGenerator = new UidGenerator();

    reserviors = bindSubstancesToReserviors(assay, array);

    operationIdToExtra = new HashMap<>();

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.priority = operation.type == OperationType.Spawn ? 1 : 2;
      extra.done = false;
      extra.active = false;

      operationIdToExtra.put(operation.id, extra);
    }

    stalledOperations = new ArrayList<>();
    activatedOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();

    List<Operation> spawnOperations = assay.getOperationsOfType(OperationType.Spawn);
    activatedOperations.addAll(spawnOperations);
    
    aliveOperationsCount += activatedOperations.size();

    timestamp = 0;

    while (true) {

      // ====================
      //     Custom Layer
      // ====================
      
      stalledOperations.addAll(activatedOperations);

      for (Iterator<Operation> it = stalledOperations.iterator(); it.hasNext();) {
        Operation stalled = it.next();
        OperationExtra stalledExtra = operationIdToExtra.get(stalled.id);
        
        // the stalled operation is already processed, which can happen if a "spawn" operation is sibling to another "spawn" operation. So just cleanup this "spawn" operation. 
        if (stalledExtra.running) {
          it.remove();
          continue;
        }
        
        if (stalled.type == OperationType.Spawn) {
          Operation descendant = stalled.outputs[0];
          
          List<Point> dropletPositions = getDropletPositions(runningDroplets);
          List<Point> reservedSpawns = new ArrayList<>();
          
          boolean canParallelSpawn = true;
          for (Operation input : descendant.inputs) {
            if (input.type == OperationType.Spawn) {
              
              // getDropletSpawn
              Point spawn = null;
              {
                outer: for (Reservior reservior : reserviors) {
                  if (!reservior.substance.equals(input.substance)) continue;
                  
                  for (Droplet droplet : runningDroplets) {
                    if (!satifiesConstraints(reservior.position, droplet.at, droplet.to)) {
                      continue outer;
                    }
                  }
                  
                  for (Point otherSpawn : reservedSpawns) {
                    if (!satisfiesSpacingConstraint(reservior.position, otherSpawn)) {
                      continue outer;
                    }
                  }
                  
                  spawn = reservior.position.copy();
                  break;
                }
              }

              if (spawn == null) {
                canParallelSpawn = false;
              } else {
                reservedSpawns.add(spawn);
                dropletPositions.add(spawn);
              }

            } else {
              OperationExtra extra = operationIdToExtra.get(input.id);

              if (!extra.done) {
                canParallelSpawn = false;
              }
            }
          }
          
          if (canParallelSpawn) {
            it.remove();

            for (Operation input : descendant.inputs) {
              
              if (input.type == OperationType.Spawn) {
                Point spawn = reservedSpawns.remove(0);

                runningOperations.add(input);
                
                Route route = new Route();
                route.start = timestamp;
                
                Droplet droplet = new Droplet();
                droplet.route = route;
                droplet.to = spawn;
                droplet.id = dropletIdGenerator.getId();
                
                runningDroplets.add(droplet);
                
                OperationExtra extra = operationIdToExtra.get(input.id);
                extra.dropletId.add(droplet.id);
                extra.running = true;
              }
            }
          }
          
        } else {
          it.remove();
          
          runningOperations.add(stalled);
          stalledExtra.running = true;

          for (Operation input : stalled.inputs) {
            OperationExtra ascendantExtra = operationIdToExtra.get(input.id);
            int ascendantDropletId = ascendantExtra.dropletId.remove(0);
            Droplet ascendantDroplet = getDroplet(ascendantDropletId, runningDroplets);
            
            stalledExtra.dropletId.add(ascendantDroplet.id);
          }
        }
      }

      stalledOperations.sort((o1, o2) -> {
        OperationExtra e1 = operationIdToExtra.get(o1.id);
        OperationExtra e2 = operationIdToExtra.get(o2.id);
        return e1.priority - e2.priority;
      });

      // choose action
      for (Operation operation : runningOperations) {
        OperationExtra extra = operationIdToExtra.get(operation.id);

        if (operation.type == OperationType.Spawn) {
          // spawn location is already selected at this point. Do nothing.
          
        } else if (operation.type == OperationType.Merge) {
          int id0 = extra.dropletId.get(0);
          int id1 = extra.dropletId.get(1);

          Droplet droplet0 = getDroplet(id0, runningDroplets);
          Droplet droplet1 = getDroplet(id1, runningDroplets);

          Point move0 = getBestMergeMove(droplet0, droplet1, runningDroplets, array);

          Point newPosition0 = droplet0.at.copy().add(move0);
          droplet0.to = newPosition0;

          Point move1 = getBestMergeMove(droplet1, droplet0, runningDroplets, array);
          Point newPosition1 = droplet1.at.copy().add(move1);
          droplet1.to = newPosition1;
          
          boolean merged = newPosition0.x == newPosition1.x && newPosition0.y == newPosition1.y;
          if (merged) {
            droplet0.to = null;
            droplet1.to = null;
            
            runningDroplets.remove(droplet0);
            runningDroplets.remove(droplet1);

            retiredDroplets.add(droplet0);
            retiredDroplets.add(droplet1);
            
            extra.dropletId.clear();

            Route route = new Route();
            route.start = timestamp;
            
            Droplet mergedDroplet = new Droplet();
            mergedDroplet.route = route;
            mergedDroplet.to = newPosition1.copy();
            mergedDroplet.id = dropletIdGenerator.getId();
            
            extra.dropletId.add(mergedDroplet.id);

            runningDroplets.add(mergedDroplet);
          }

        } else if (operation.type == OperationType.Split) {
          
          int id = extra.dropletId.get(0);
          
          Droplet droplet = getDroplet(id, runningDroplets);

          boolean horizontalSplit = true;
          
          Point left = new Point(-1, 0).add(droplet.at);
          Point right = new Point(1, 0).add(droplet.at);
          
          if (!inside(left.x, left.y, array.width, array.height)) horizontalSplit = false;
          if (!inside(right.x, right.y, array.width, array.height)) horizontalSplit = false;
          
          for (Droplet other : runningDroplets) {
            if (other.id == droplet.id) continue;
            
            boolean ok1 = mutuallySatifiesConstraints(droplet.at, left, other.at, other.to);
            boolean ok2 = mutuallySatifiesConstraints(droplet.at, right, other.at, other.to);
            
            if (!ok1 || !ok2) horizontalSplit = false;
          }

          boolean verticalSplit = true;
          
          Point up = new Point(0, 1).add(droplet.at);
          Point down = new Point(0, -1).add(droplet.at);
          
          if (!inside(up.x, up.y, array.width, array.height)) verticalSplit = false;
          if (!inside(down.x, down.y, array.width, array.height)) verticalSplit = false;
          
          for (Droplet other : runningDroplets) {
            if (other.id == droplet.id) continue;
            
            boolean ok1 = mutuallySatifiesConstraints(droplet.at, up, other.at, other.to);
            boolean ok2 = mutuallySatifiesConstraints(droplet.at, down, other.at, other.to);
            
            if (!ok1 || !ok2) verticalSplit = false;
          }

          Point to1 = verticalSplit ? up : left;
          Point to2 = verticalSplit ? down : right;
          
          boolean split = verticalSplit || horizontalSplit;
          if (split) {
            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            extra.dropletId.clear();

            Route r1 = new Route();
            r1.start = timestamp;
            
            Droplet s1 = new Droplet();
            s1.route = r1;
            s1.to = to1;
            s1.id = dropletIdGenerator.getId();
            
            Route r2 = new Route();
            r2.start = timestamp;
            
            Droplet s2 = new Droplet();
            s2.route = r2;
            s2.to = to2;
            s2.id = dropletIdGenerator.getId();
            
            runningDroplets.add(s1);
            runningDroplets.add(s2);
            
            extra.dropletId.add(s1.id);
            extra.dropletId.add(s2.id);
          } else {
            // move somewhere, where it can split.
            Point move = getBestSplitMove(droplet, runningDroplets, array);
            if (move == null) throw new IllegalStateException("broken!");
            
            droplet.to = new Point(droplet.at).add(move);
          }
        } else if (operation.type == OperationType.Mix) {
          int id = extra.dropletId.get(0);
          Droplet droplet = getDroplet(id, runningDroplets);
          
          Point move = getBestMixMove(droplet, percentages, array);
          droplet.to = new Point(droplet.at).add(move);
          
        } else {
          throw new IllegalStateException("unsupported operation!");
        }
      }

      // ====================

      for (Operation operation : runningOperations) {
        if (operation.type != OperationType.Mix) continue;
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        int dropletId = extra.dropletId.get(0);
        Droplet droplet = getDroplet(dropletId, runningDroplets);
        
        Point previousMove = getPreviousMove(droplet);
        Point move = droplet.to.copy().sub(droplet.at);
        
        float mixing = getMixingPercentage(move, previousMove, percentages);
        extra.mixingPercentage += mixing;
        if (extra.mixingPercentage > 100) extra.mixingPercentage = 100;
      }
      
      // perform action
      for (Droplet droplet : runningDroplets) {
        Point next = (droplet.to == null) ? droplet.at.copy() : droplet.to;
        droplet.route.path.add(next);
        droplet.at = next;
        droplet.to = null;
      }

      timestamp += 1;

      // cleanup done operations and queue descended operations
      activatedOperations.clear();

      for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (operation.type == OperationType.Spawn) {
          int id = extra.dropletId.get(0);
          
          Droplet droplet = getDroplet(id, runningDroplets);
          if (droplet.at != null) extra.done = true;
          
        } else if (operation.type == OperationType.Merge) {
          extra.done = (extra.dropletId.size() == 1);
        } else if (operation.type == OperationType.Split) {
          extra.done = (extra.dropletId.size() == 2);
        } else if (operation.type == OperationType.Mix) {
          extra.done = (extra.mixingPercentage == 100);
        } else {
          throw new IllegalStateException("unsupported operation!");
        }

        if (extra.done) {
          it.remove();
          aliveOperationsCount -= 1;

          System.out.printf("completed %d (%s)\n", operation.id, operation.type);

          for (Operation descendant : operation.outputs) {
            if (descendant == null) continue;

            OperationExtra descendantExtra = operationIdToExtra.get(descendant.id);
            if (descendantExtra.active) continue;

            boolean canRun = true;
            for (Operation input : descendant.inputs) {
              OperationExtra inputExtra = operationIdToExtra.get(input.id);
              if (!inputExtra.done) canRun = false;
            }

            if (canRun) {
              descendantExtra.active = true;
              activatedOperations.add(descendant);
              aliveOperationsCount += 1;
            }
          }
        }
      }
      
      if (aliveOperationsCount == 0) break;

    }
    
    retiredDroplets.addAll(runningDroplets);
    runningDroplets.clear();

    List<Route> routes = new ArrayList<>();
    for (Droplet droplet : retiredDroplets) {
      routes.add(droplet.route);
    }

    return routes;
  }
  
  private float getMixingPercentage(Point move, Point previousMove, MixingPercentages mixingPercentages) {
    if (previousMove == null) {
      boolean stay1 = (move.x == 0 && move.y == 0);
      if (stay1) return mixingPercentages.stationaryPercentage;
      return mixingPercentages.firstPercentage;
    } else {

      boolean stay1 = (move.x == 0 && move.y == 0);
      boolean left1 = (move.x == -1 && move.y == 0);
      boolean right1 = (move.x == 1 && move.y == 0);
      boolean up1 = (move.x == 0 && move.y == 1);
      boolean down1 = (move.x == 0 && move.y == -1);
      
      boolean stay0 = (previousMove.x == 0 && previousMove.y == 0);
      boolean left0 = (previousMove.x == -1 && previousMove.y == 0);
      boolean right0 = (previousMove.x == 1 && previousMove.y == 0);
      boolean up0 = (previousMove.x == 0 && previousMove.y == 1);
      boolean down0 = (previousMove.x == 0 && previousMove.y == -1);
      
      if (stay1) return mixingPercentages.stationaryPercentage;
      
      if (!stay1 && stay0) return mixingPercentages.firstPercentage;

      if (left1 && left0) return mixingPercentages.forwardPercentage;
      if (right1 && right0) return mixingPercentages.forwardPercentage;
      if (up1 && up0) return mixingPercentages.forwardPercentage;
      if (down1 && down0) return mixingPercentages.forwardPercentage;
      
      if (left1 && right0) return mixingPercentages.reversePercentage;
      if (right1 && left0) return mixingPercentages.reversePercentage;
      if (up1 && down0) return mixingPercentages.reversePercentage;
      if (down1 && up0) return mixingPercentages.reversePercentage;
      
      if ((up1 || down1) && (right0 || left0)) return mixingPercentages.turnPercentage;
      if ((left1 || right1) && (up0 || down0)) return mixingPercentages.turnPercentage;
      
      throw new IllegalStateException("broken! forgot mixing percentage.");
    }
  }

  private Point getBestMixMove(Droplet droplet, MixingPercentages mixingPercentages, BioArray array) {
    List<Point> validMoves = getValidMoves(droplet, null, timestamp, runningDroplets, array);
    Point prevMove = getPreviousMove(droplet);
    
    float bestPercentage = Float.MIN_VALUE;
    Point bestMove = null;
    
    for (Point move : validMoves) {
      float percentage = getMixingPercentage(move, prevMove, mixingPercentages);
      
      if (percentage > bestPercentage) {
        bestPercentage = percentage;
        bestMove = move;
      }
    }
    
    return bestMove;
  }

  private Point getPreviousMove(Droplet droplet) {
    List<Point> path = droplet.route.path;
    if (path.size() >= 2) {
      Point at0 = path.get(path.size() - 2);
      Point at1 = path.get(path.size() - 1);
      
      Point prevMove = new Point();
      prevMove.set(at1).sub(at0);
      
      return prevMove;
    }
    
    return null;
  }

  private List<Point> getValidMoves(Droplet droplet, Droplet mergeSibling, int timestamp, List<Droplet> droplets, BioArray array) {
    List<Point> candidateMoves = new ArrayList<>();
    candidateMoves.add(new Point(-1, 0));
    candidateMoves.add(new Point(1, 0));
    candidateMoves.add(new Point(0, 1));
    candidateMoves.add(new Point(0, -1));
    candidateMoves.add(new Point(0, 0));
    
    Point next = new Point();
    
    List<Point> validMoves = new ArrayList<>();
    outer: for (Point move : candidateMoves) {
      next.set(droplet.at).add(move);
      
      if (!inside(next.x, next.y, array.width, array.height)) continue;

      for (Droplet other : droplets) {
        if (other.id == droplet.id) continue;
        if (mergeSibling != null && other.id == mergeSibling.id) continue;
        
        if (!mutuallySatifiesConstraints(droplet.at, next, other.at, other.to)) continue outer;
      }
      
      if (mergeSibling != null) {
        if (!mutuallySatisfiesCompanionConstraints(droplet.at, next, mergeSibling.at, mergeSibling.to)) continue;
      }
      
      validMoves.add(move);
    }
    
    return validMoves;
  }

  private Droplet getDroplet(int id, List<Droplet> droplets) {
    for (Droplet droplet : droplets) {
      if (id == droplet.id) return droplet;
    }
    throw new IllegalStateException("no droplet with id: " + id);
  }

  private List<Point> getDropletPositions(List<Droplet> droplets) {
    List<Point> positions = new ArrayList<>();

    for (Droplet droplet : droplets) {
      positions.add(droplet.at);
    }

    return positions;
  }

  private List<Reservior> bindSubstancesToReserviors(BioAssay assay, BioArray array) {
    List<Operation> inputoperations = assay.getOperationsOfType(OperationType.Spawn);
    List<Point> reserviorTiles = array.reserviorTiles;

    List<String> assigned = new ArrayList<>();
    List<String> pending = new ArrayList<>();

    List<Reservior> reserviors = new ArrayList<>();

    int reserviorIndex = 0;
    int inputIndex = 0;

    while (inputIndex < inputoperations.size()) {
      Operation inputoperation = inputoperations.get(inputIndex);
      inputIndex += 1;

      if (assigned.contains(inputoperation.substance)) {
        pending.add(inputoperation.substance);
      } else {
        assigned.add(inputoperation.substance);

        Point reserviorTile = reserviorTiles.get(reserviorIndex);
        reserviorIndex += 1;

        Reservior reservior = new Reservior();
        reservior.substance = inputoperation.substance;
        reservior.position = reserviorTile.copy();
        reserviors.add(reservior);

        if (reserviorIndex > reserviorTiles.size()) {
          throw new IllegalStateException("not enough reservior tiles!");
        }
      }
    }

    while (reserviorIndex < reserviorTiles.size() && pending.size() > 0) {
      String substance = pending.remove(0);

      Point reserviorTile = reserviorTiles.get(reserviorIndex);
      reserviorIndex += 1;

      Reservior reservior = new Reservior();
      reservior.substance = substance;
      reservior.position = reserviorTile.copy();
      reserviors.add(reservior);
    }

    for (Reservior reservior : reserviors) {
      System.out.printf("reservior %s: %s\n", reservior.position, reservior.substance);
    }

    return reserviors;
  }

  private Point getDropletSpawn(String substance, List<Reservior> reserviors, List<Point> otherDroplets) {
    outer: for (Reservior reservior : reserviors) {
      if (!reservior.substance.equals(substance)) continue;
      
      for (Point other : otherDroplets) {
        if (other.x == reservior.position.x && other.y == reservior.position.y) continue outer;
      }

      return reservior.position.copy();
    }

    // throw new IllegalStateException("Could not spawn droplet. The tile is
    // occupied or substance reservior does not exist!");
    return null;
  }

  private Point getBestMergeMove(Droplet droplet, Droplet toMerge, List<Droplet> droplets, BioArray array) {
    List<Point> validMoves = getValidMoves(droplet, toMerge, timestamp, droplets, array);
    
    Point best = null;
    int shortestDistance = Integer.MAX_VALUE;

    Point at = droplet.at;
    Point target = (toMerge.to == null) ? toMerge.at : toMerge.to;

    for (Point move : validMoves) {
      Point next = new Point(at).add(move);

      int distance = getManhattenDistance(next.x, next.y, target.x, target.y);

      if (distance < shortestDistance) {
        shortestDistance = distance;
        best = move;
      }
    }

    return best;
  }

  
  private Point getBestSplitMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Point bestMove = null;
    int longestDistance = 0;

    List<Point> validMoves = getValidMoves(droplet, null, timestamp, droplets, array);

    // select move which is furthest away from wall corner.
    for (Point move : validMoves) {
      Point to = new Point(droplet.at).add(move);

      int distance1 = getManhattenDistance(to.x, to.y, 0, 0);
      int distance2 = getManhattenDistance(to.x, to.y, array.width - 1, 0);
      int distance3 = getManhattenDistance(to.x, to.y, 0, array.height - 1);
      int distance4 = getManhattenDistance(to.x, to.y, array.width - 1, array.height - 1);

      int minimumDistance = Math.min(Math.min(distance1, distance2), Math.min(distance3, distance4));
      
      if (minimumDistance >= longestDistance) {
        longestDistance = minimumDistance;
        bestMove = move;
      }
    }

    return bestMove;
  }

  private int getManhattenDistance(int sx, int sy, int tx, int ty) {
    return Math.abs(sx - tx) + Math.abs(sy - ty);
  }
  
  public boolean satisfiesSpacingConstraint(Point p1, Point p2) {
    // If 1 or more points are null, then we assume that those points are not placed. Thus, the placement is valid, because the points do not interfere. @docs
    if (p1 == null || p2 == null) return true;  
    
    int dx = Math.abs(p1.x - p2.x);
    int dy = Math.abs(p1.y - p2.y);

    return dx >= 2 || dy >= 2;
  }
  
  public boolean satifiesConstraints(Point to0, Point at1, Point to1) {
    // dynamic constraint
    if (!satisfiesSpacingConstraint(to0, at1)) return false;
    
    // static constraint
    if (!satisfiesSpacingConstraint(to0, to1)) return false;
    
    return true;
  }
  
  public boolean mutuallySatifiesConstraints(Point at0, Point to0, Point at1, Point to1) {
    // dynamic constraint
    if (!satisfiesSpacingConstraint(to0, at1)) return false;
    if (!satisfiesSpacingConstraint(to1, at0)) return false;
    
    // static constraint
    if (!satisfiesSpacingConstraint(to0, to1)) return false;
    
    return true;
  }
  
  public boolean mutuallySatisfiesCompanionConstraints(Point at0, Point to0, Point at1, Point to1) {
    if (mutuallySatifiesConstraints(at0, to0, at1, to1)) return true;
    
    if (to0 != null && at1 != null) {
      int dx = Math.abs(to0.x - at1.x);
      int dy = Math.abs(to0.y - at1.y);
      
      // we assume that if there is 1 spacing in "time", then the other one will do a move which handles the problem through a split or merge.
      boolean dynamicOk = (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
      if (!dynamicOk) return false;
    }
    
    if (to1 != null && at0 != null) {
      int dx = Math.abs(to1.x - at0.x);
      int dy = Math.abs(to1.y - at0.y);
      
      // we assume that if there is 1 spacing in "time", then the other one will do a move which handles the problem through a split or merge.
      boolean dynamicOk = (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
      if (!dynamicOk) return false;
    }
      
    if (to0 != null && to1 != null) {
      // special case for a merge and split points can't be next to each other, but they may overlap.
      
      // Illegal:
      // o o o o
      // o x x o
      // o o o o
      int dx = Math.abs(to0.x - to1.x);
      int dy = Math.abs(to0.y - to1.y);
      
      boolean staticOk = dx == 0 && dy == 0;
      if (!staticOk) return false;
      
    }
    
    return true;
  }

  private boolean inside(int x, int y, int width, int height) {
    return x >= 0 && x <= width - 1 && y >= 0 && y <= height - 1;
  }

}

class Reservior { // global
  public Point position;
  public String substance;
}

class Droplet { // global?
  public int id;

  public Point at; // at is not always the last element of the route, because when we do a spawn of a descended, then we ignore the first position.
  public Point to;
  
  public Route route;
}

class OperationExtra { // algorithm specific
  public List<Integer> dropletId = new ArrayList<>();

  public int priority;

  public boolean active;
  public boolean running;
  public boolean done;
  
  public float mixingPercentage;  // only used for mixing operations.
}