package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MergeRouter {

  private List<Operation> baseOperations;
  private List<Operation> runningOperations;
  
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
      operationIdToExtra.put(operation.id, extra);
    }

    baseOperations = assay.getOperationalBase();
    runningOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();
    
    // @remove
    /*
    List<Droplet> temporaryDroplets = new ArrayList<>();
    {
      Route froute = new Route();
      froute.path.add(new Point(3, 0));
  
      Droplet filler = new Droplet();
      filler.id = -1;
      filler.route = froute;
      runningDroplets.add(filler);
      temporaryDroplets.add(filler);
    }
    */

    int timestamp = 0;
    
    while (true) {

      for (Iterator<Operation> it = baseOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();
        
        boolean canRun = true;
        List<Point> dropletPositions = getDropletPositions(runningDroplets);
        List<Point> spawns = new ArrayList<>();
        for (Operation input : operation.inputs) {
          Point spawn = getDropletSpawn(input.substance, reserviors, dropletPositions);

          if (spawn == null) {
            canRun = false;
          } else {
            spawns.add(spawn);
            dropletPositions.add(spawn);
          }
        }

        if (canRun) {
          it.remove();
          runningOperations.add(operation);

          System.out.printf("spawning %d (%s)\n", operation.id, operation.type);
          
          for (Point position : spawns) {
            Droplet droplet = createDroplet(position, timestamp);
            runningDroplets.add(droplet);
            
            OperationExtra extra = operationIdToExtra.get(operation.id);
            extra.dropletId.add(droplet.id);
          }
        }
      }

      List<Operation> queuedOperations = new ArrayList<>();

      for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();

        OperationExtra extra = operationIdToExtra.get(operation.id);

        if ("merge".equals(operation.type)) {
          int id0 = extra.dropletId.get(0);
          int id1 = extra.dropletId.get(1);

          Droplet droplet0 = getDroplet(id0, runningDroplets);
          Droplet droplet1 = getDroplet(id1, runningDroplets);

          Point position0 = getPosition(droplet0);
          Point position1 = getPosition(droplet1);

          boolean merged = position0.x == position1.x && position0.y == position1.y;
          if (merged) {
            it.remove();

            extra.done = true;

            retiredDroplets.add(droplet0);
            runningDroplets.remove(droplet0);

            retiredDroplets.add(droplet1);
            runningDroplets.remove(droplet1);

            System.out.printf("completed %d (%s)\n", operation.id, operation.type);

          } else { // do a merge move

            Point move0 = getBestMergeMove(droplet0, droplet1, runningDroplets, array);
            Point newPosition0 = getPosition(droplet0).copy().add(move0);
            droplet0.to = newPosition0;

            Point move1 = getBestMergeMove(droplet1, droplet0, runningDroplets, array);
            Point newPosition1 = getPosition(droplet1).copy().add(move1);
            droplet1.to = newPosition1;

            // System.out.println("pos0: " + newPosition0 + ", pos1: " + newPosition1);
          }

        } else {
          throw new IllegalStateException("unsupported operation!");
        }

        if (extra.done) {
          for (Operation readyoperation : operation.outputs) {
            List<Operation> inputSpawnOperations = new ArrayList<>();
            List<Operation> inputOperationaloperations = new ArrayList<>();

            for (Operation input : readyoperation.inputs) {
              if ("input".equals(input.type)) {
                inputSpawnOperations.add(input);
              } else {
                inputOperationaloperations.add(input);
              }
            }

            boolean canRun = true;
            for (Operation operationaloperation : inputOperationaloperations) {
              OperationExtra opExtra = operationIdToExtra.get(operationaloperation.id);
              if (!opExtra.done) canRun = false;
            }

            // try and spawn all input operations.
            List<Point> allDroplets = getDropletPositions(runningDroplets);
            List<Point> spawns = new ArrayList<>();
            for (Operation input : inputSpawnOperations) {
              Point spawn = getDropletSpawn(input.substance, reserviors, allDroplets);

              if (spawn == null) {
                canRun = false;
              } else {
                spawns.add(spawn);
                allDroplets.add(spawn);
              }
            }

            if (canRun) {
              System.out.printf("starting %d (%s)\n", readyoperation.id, readyoperation.type);

              OperationExtra readyOperationExtra = operationIdToExtra.get(readyoperation.id);

              if ("merge".equals(readyoperation.type)) {

                for (Point spawn : spawns) {
                  Droplet droplet = createDroplet(spawn, timestamp);
                  
                  runningDroplets.add(droplet);
                  readyOperationExtra.dropletId.add(droplet.id);
                }

                for (Operation op : inputOperationaloperations) {
                  // :forwardIndex
                  // @incomplete: assumes no splitting for now!
                  OperationExtra prevoperationExtra = operationIdToExtra.get(op.id);
                  int prevDropletId = prevoperationExtra.dropletId.get(0);

                  Point spawn = getPosition(getDroplet(prevDropletId, retiredDroplets));
                  Droplet droplet = createDroplet(spawn, timestamp);
                  runningDroplets.add(droplet);
                  readyOperationExtra.dropletId.add(droplet.id);
                }

                queuedOperations.add(readyoperation);

              } else if ("sink".equals(readyoperation.type)) {
                System.out.println("sink...");
              } else {
                throw new IllegalStateException("unsupported operation! " + readyoperation.type);
              }
            }
          }
        }
      }

      runningOperations.addAll(queuedOperations);

      // execute the move.
      for (Droplet droplet : runningDroplets) {
        Point next = droplet.to;
        if (next == null) next = getPosition(droplet).copy();
        droplet.to = null;

        droplet.route.path.add(next);
      }

      if (runningOperations.size() == 0 && baseOperations.size() == 0) break;

      timestamp += 1;

    }

    List<Route> routes = new ArrayList<>();
    for (Droplet droplet : retiredDroplets) {
      routes.add(droplet.route);
    }
    
    /*
    // @TODO: remove: these droplets are manually added to test stuff.
    for (Droplet droplet : temporaryDroplets) {
      routes.add(droplet.route);
    }
    */

    return routes;
  }

  private Droplet createDroplet(Point spawn, int timestamp) {
    Route route = new Route();
    route.path.add(spawn);
    route.start = timestamp;

    Droplet droplet = new Droplet();
    droplet.route = route;
    droplet.id = nextDropletId;
    nextDropletId += 1;
    
    return droplet;
  }

  private Point getPosition(Droplet droplet) {
    return droplet.route.path.get(droplet.route.path.size() - 1);
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
      List<Point> path = droplet.route.path;
      positions.add(path.get(path.size() - 1));
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

    Point at = getPosition(droplet);
    Point target = (toMerge.to == null) ? getPosition(toMerge) : toMerge.to;

    for (Point dt : candidates) {
      Point next = new Point();
      next.x = at.x + dt.x;
      next.y = at.y + dt.y;

      if (!inside(next.x, next.y, array.width, array.height)) continue;
      if (!validMove(droplet, next, droplets, toMerge)) continue;

      int distance = Math.abs(next.x - target.x) + Math.abs(next.y - target.y);

      if (distance < shortestDistance) {
        shortestDistance = distance;
        best = dt;
      }
    }

    return best;
  }

  public boolean validMove(Droplet droplet, Point to, List<Droplet> droplets, Droplet companion) {
    for (Droplet other : droplets) {
      if (droplet.id == other.id) continue;
      if (companion != null && companion.id == other.id) continue;

      { // dynamic constraint
        Point otherAt = getPosition(other);

        int dx = Math.abs(to.x - otherAt.x);
        int dy = Math.abs(to.y - otherAt.y);

        boolean dynamicOk = dx >= 2 || dy >= 2;
        if (!dynamicOk) return false;
      }

      { // static constraint
        if (other.to != null) {
          int dx = Math.abs(to.x - other.to.x);
          int dy = Math.abs(to.y - other.to.y);

          boolean staticOk = dx >= 2 || dy >= 2;
          if (!staticOk) return false;
        }
      }
    }
    
    {
      if (companion != null) {
        {
          Point otherAt = getPosition(companion);
          
          int dx = Math.abs(to.x - otherAt.x);
          int dy = Math.abs(to.y - otherAt.y);
    
          // we assume that if there is 1 spacing in "time", then the other one will do a move
          // which handles the problem through a split or merge.
          boolean dynamicOk = dx >= 2 || dy >= 2;
          dynamicOk |= (dx == 0 && dy == 0);  // split
          dynamicOk |= (dx == 1 && dy == 0) || (dx == 0 && dy == 1);  // merge
          if (!dynamicOk) return false;
        }
        
        {
          if (companion.to != null) {
            // special case for a merge and split
            // points can't be next to each other, but they may overlap.
  
            // Illegal:
            // o o o o
            // o x x o
            // o o o o
            int dx = Math.abs(to.x - companion.to.x);
            int dy = Math.abs(to.y - companion.to.y);
  
            boolean staticOk = dx >= 2 || dy >= 2;
            staticOk |= dx == 0 && dy == 0;
            if (!staticOk) return false;
          }
         }
      }
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
  public Route route;
  
  public Point to;
}

class OperationExtra { // algorithm specific
  public List<Integer> dropletId = new ArrayList<>();

  public boolean done; // @NOTE: input operations do not set this true currently
  // public int forwardIndex; // this becomes relevant when an input is a split,
  // as some droplets after a split may be assigned as inputs already.
}
