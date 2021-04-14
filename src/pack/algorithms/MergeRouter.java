package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MergeRouter {

  private List<Operation> stalledOperations;  // operation which should be ready but are not executing for some reason, i.e. reservior is blocked.
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
      extra.priority = operation.type.equals("input") ? 1 : 2;
      extra.done = false;
      extra.running = false;
      
      operationIdToExtra.put(operation.id, extra);
    }

    runningOperations = new ArrayList<>();
    stalledOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();
    
    List<Operation> baseOperations = assay.getOperationalBase();
    stalledOperations.addAll(baseOperations);
    
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
      
      // try and start the input "spawn" operation(s) of stalled operations.
      for (Iterator<Operation> it = stalledOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();

        List<Point> dropletPositions = getDropletPositions(runningDroplets);
        List<Point> reservedSpawns = new ArrayList<>();
        
        boolean canParallelSpawn = true;
        for (Operation input : operation.inputs) {
          
          if(input.type.equals("input")) {
            Point spawn = getDropletSpawn(input.substance, reserviors, dropletPositions);
            
            if (spawn == null) {
              canParallelSpawn = false;
            } else {
              reservedSpawns.add(spawn);
              dropletPositions.add(spawn);
            }
            
          } else {
            OperationExtra extra = operationIdToExtra.get(input.id);
            
            if (!extra.done) {
              throw new IllegalStateException("broken! inputs which are non-spawn operations should be done.");
            }
          }
        }

        if (canParallelSpawn) {
          it.remove();
          
          for (int i = 0; i < operation.inputs.size(); i++) {
            Operation spawnOperation = operation.inputs.get(i);
            if (!spawnOperation.type.equals("input")) continue;
            
            Point spawn = reservedSpawns.remove(0);
            
            runningOperations.add(spawnOperation);
            
            Route route = new Route();
            route.start = timestamp;

            Droplet droplet = new Droplet();
            droplet.route = route;
            droplet.at = spawn;
            droplet.id = nextDropletId;
            nextDropletId += 1;
            
            runningDroplets.add(droplet);
            
            OperationExtra extra = operationIdToExtra.get(spawnOperation.id);
            extra.dropletId.add(droplet.id);
            extra.running = true;
            
          }
        }
      }
      
      runningOperations.sort((o1, o2) -> {
        OperationExtra e1 = operationIdToExtra.get(o1.id);
        OperationExtra e2 = operationIdToExtra.get(o2.id);
        return e1.priority - e2.priority;
      });

      // choose action
      for (Operation operation : runningOperations) {
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if ("input".equals(operation.type)) {
          int id = extra.dropletId.get(0);
          Droplet droplet = getDroplet(id, runningDroplets);
          droplet.to = droplet.at;
          
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

        } else {
          throw new IllegalStateException("unsupported operation!");
        }
      }

      // perform action
      for (Droplet droplet : runningDroplets) {
        Point next = (droplet.to == null) ? droplet.at.copy() : droplet.to;
        droplet.route.path.add(next);
        droplet.at = next;
        droplet.to = null;
      }
      
      timestamp += 1;
      
      // cleanup completed operations
      List<Operation> completedOperations = new ArrayList<>();
      for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (operation.type.equals("input")) {
          int id = extra.dropletId.get(0);
          
          Droplet droplet = getDroplet(id, runningDroplets);

          runningDroplets.remove(droplet);
          retiredDroplets.add(droplet);
          
          extra.done = true;
          completedOperations.add(operation);
          
          it.remove();
          
          System.out.printf("completed %d (%s)\n", operation.id, operation.type);
          
        } else if (operation.type.equals("merge")) {
          int id0 = extra.dropletId.get(0);
          int id1 = extra.dropletId.get(1);

          Droplet droplet0 = getDroplet(id0, runningDroplets);
          Droplet droplet1 = getDroplet(id1, runningDroplets);

          Point position0 = droplet0.at;
          Point position1 = droplet1.at;
          
          // System.out.println("pos0: " + newPosition0 + ", pos1: " + newPosition1);
          boolean merged = position0.x == position1.x && position0.y == position1.y;
          if (merged) {
            runningDroplets.remove(droplet0);
            runningDroplets.remove(droplet1);

            retiredDroplets.add(droplet0);
            retiredDroplets.add(droplet1);
            
            extra.done = true;
            completedOperations.add(operation);
            
            it.remove();
            
            System.out.printf("completed %d (%s)\n", operation.id, operation.type);
          }
          
        } else {
          throw new IllegalStateException("unsupported operation.");
        }
      }
      
      // queue descended operations
      for (Operation completed : completedOperations) {
        
        for (Operation descendant : completed.outputs) {
          OperationExtra descendantExtra = operationIdToExtra.get(descendant.id);
          if (descendantExtra.running) continue;
          if (descendant.type.equals("sink")) continue;

          boolean canRun = true;
          boolean isNonSpawnInputDone = true;
          
          for (Operation input : descendant.inputs) {
            OperationExtra inputExtra = operationIdToExtra.get(input.id);
            if (!inputExtra.done) {
              canRun = false;

              if (!input.type.equals("input")) isNonSpawnInputDone = false;
            }
          }
        
          if (isNonSpawnInputDone && !canRun) stalledOperations.add(descendant);
     
          if (canRun) {
            runningOperations.add(descendant);
            descendantExtra.running = true;
            
            for (Operation input : descendant.inputs) {
              
              // :forwardIndex
              // @incomplete: assumes no splitting for now!
              OperationExtra prevExtra = operationIdToExtra.get(input.id);
              int prevDropletId = prevExtra.dropletId.get(0);

              Droplet prevDroplet = getDroplet(prevDropletId, retiredDroplets);
              
              Route route = new Route();
              route.start = timestamp;

              Droplet droplet = new Droplet();
              droplet.route = route;
              droplet.at = prevDroplet.at;
              droplet.id = nextDropletId;
              nextDropletId += 1;
              
              runningDroplets.add(droplet);
              
              descendantExtra.dropletId.add(droplet.id);
            }
          }
        }
      }
          
      if (runningOperations.size() == 0 && stalledOperations.size() == 0) break;
      
    }

    // @TODO: post process combine a spawn operation route with the following route.
    List<Route> routes = new ArrayList<>();
    for (Droplet droplet : retiredDroplets) {
      routes.add(droplet.route);
    }
    
    /*
    // @TODO: @remove: these droplets are manually added to test stuff.
    for (Droplet droplet : temporaryDroplets) {
      routes.add(droplet.route);
    }
    */

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
        Point otherAt = other.at;

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
          Point otherAt = companion.at;
          
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
  
  public Point at;  // at is not always the last element of the route, because when we do a spawn of a descended, then we ignore the first position.
  public Point to;
  
  public Route route;
}

class OperationExtra { // algorithm specific
  public List<Integer> dropletId = new ArrayList<>();

  public int priority;

  public boolean running;
  public boolean done; // @NOTE: input operations do not set this true currently
  // public int forwardIndex; // this becomes relevant when an input is a split,
  // as some droplets after a split may be assigned as inputs already.
}

