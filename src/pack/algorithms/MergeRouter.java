package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MergeRouter {

  private List<Operation> stalledOperations;
  private List<Operation> activatedOperations;
  private List<Operation> runningOperations;
  
  private int aliveOperationsCount;

  private List<Droplet> runningDroplets;
  private List<Droplet> retiredDroplets;

  private List<Reservior> reserviors;

  private Map<Integer, OperationExtra> operationIdToExtra;

  int nextDropletId;

  public List<Route> compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    reserviors = bindSubstancesToReserviors(assay, array);

    operationIdToExtra = new HashMap<>();

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.priority = operation.type.equals("input") ? 1 : 2;
      extra.done = false;
      extra.active = false;

      operationIdToExtra.put(operation.id, extra);
    }

    stalledOperations = new ArrayList<>();
    activatedOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();

    List<Operation> spawnOperations = assay.getOperationsOfType("input");
    activatedOperations.addAll(spawnOperations);
    
    aliveOperationsCount += activatedOperations.size();

    int timestamp = 0;

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
        
        if (stalled.type.equals("input")) {
          Operation descendant = stalled.outputs.get(0);
          
          List<Point> dropletPositions = getDropletPositions(runningDroplets);
          List<Point> reservedSpawns = new ArrayList<>();
          
          boolean canParallelSpawn = true;
          for (Operation input : descendant.inputs) {
            if (input.type.equals("input")) {
              
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
              
              if (input.type.equals("input")) {
                Point spawn = reservedSpawns.remove(0);

                runningOperations.add(input);
                
                Route route = new Route();
                route.start = timestamp;
                
                Droplet droplet = new Droplet();
                droplet.route = route;
                droplet.to = spawn;
                droplet.id = nextDropletId;
                nextDropletId += 1;
                
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
            // :forwardIndex
            // @incomplete: assumes no splitting for now!
            OperationExtra ascendantExtra = operationIdToExtra.get(input.id);
            int ascendantDropletId = ascendantExtra.dropletId.get(0);
            Droplet ascendantDroplet = getDroplet(ascendantDropletId, runningDroplets);
            runningDroplets.remove(ascendantDroplet);
            retiredDroplets.add(ascendantDroplet);
            
            Route route = new Route();
            route.start = timestamp;
            
            Droplet droplet = new Droplet();
            droplet.route = route;
            droplet.at = ascendantDroplet.at; // @fix: at vs to in general when to use each.
            droplet.id = nextDropletId;
            nextDropletId += 1;
            
            stalledExtra.dropletId.add(droplet.id);
            runningDroplets.add(droplet);
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

        if ("input".equals(operation.type)) {
          // spawn location is already selected at this point. Do nothing.
          
        } else if ("merge".equals(operation.type)) {
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
            mergedDroplet.id = nextDropletId;
            nextDropletId += 1;
            
            extra.dropletId.add(mergedDroplet.id);

            runningDroplets.add(mergedDroplet);
          }

        } else if ("split".equals(operation.type)) {
          // @incomplete
          int id = extra.dropletId.get(0);
          
          Droplet droplet = getDroplet(id, runningDroplets);

          boolean split = false;
          if (split) {
            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            extra.dropletId.clear();

            Droplet s1 = new Droplet();
            s1.to = null;
            
            Droplet s2 = new Droplet();
            s2.to = null;
            
            runningDroplets.add(s1);
            runningDroplets.add(s2);
            
            extra.dropletId.add(s1.id);
            extra.dropletId.add(s2.id);
            
          } else {
            // move somewhere, where it can split.
          }
          
        } else {
          throw new IllegalStateException("unsupported operation!");
        }
      }

      // ====================

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
        
        if ("input".equals(operation.type)) {
          int id = extra.dropletId.get(0);
          
          Droplet droplet = getDroplet(id, runningDroplets);
          if (droplet.at != null) extra.done = true;
          
        } else if ("merge".equals(operation.type)) {
          extra.done = (extra.dropletId.size() == 1);
          
        } else {
          throw new IllegalStateException("unsupported operation!");
        }

        if (extra.done) {
          it.remove();
          aliveOperationsCount -= 1;

          System.out.printf("completed %d (%s)\n", operation.id, operation.type);

          for (Operation descendant : operation.outputs) {
            OperationExtra descendantExtra = operationIdToExtra.get(descendant.id);
            if (descendantExtra.active) continue;
            if (descendant.type.equals("sink")) continue;

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

    // @TODO: post process combine a spawn operation route with the following route.
    List<Route> routes = new ArrayList<>();
    for (Droplet droplet : retiredDroplets) {
      routes.add(droplet.route);
    }

    return routes;
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
    List<Operation> inputoperations = assay.getOperationsOfType("input");
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
    List<Point> candidates = new ArrayList<>();
    candidates.add(new Point(-1, 0));
    candidates.add(new Point(1, 0));
    candidates.add(new Point(0, 1));
    candidates.add(new Point(0, -1));
    candidates.add(new Point(0, 0));

    Point best = null;
    int shortestDistance = Integer.MAX_VALUE;

    Point at = droplet.at;
    Point target = (toMerge.to == null) ? toMerge.at : toMerge.to;

    for (Point dt : candidates) {
      Point next = new Point();
      next.x = at.x + dt.x;
      next.y = at.y + dt.y;

      if (!inside(next.x, next.y, array.width, array.height)) continue;
      if (!validMergeMove(droplet, next, droplets, toMerge)) continue;

      int distance = getManhattenDistance(next.x, next.y, target.x, target.y);

      if (distance < shortestDistance) {
        shortestDistance = distance;
        best = dt;
      }
    }

    return best;
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

  public boolean validMergeMove(Droplet droplet, Point to, List<Droplet> droplets, Droplet companion) {
    for (Droplet other : droplets) {
      if (droplet.id == other.id) continue;
      if (companion != null && companion.id == other.id) continue;
      
      if (!mutuallySatifiesConstraints(droplet.at, to, other.at, other.to)) return false;
    }
    
    if (companion != null) {
      if (!mutuallySatisfiesCompanionConstraints(droplet.at, to, companion.at, companion.to)) return false;
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
  public boolean done; // @NOTE: input operations do not set this true currently
  // public int forwardIndex; // this becomes relevant when an input is a split,
  // as some droplets after a split may be assigned as inputs already.
}