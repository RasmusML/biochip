package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import engine.math.MathUtils;

public class GreedyRouter {
  
  private BioConstraintsChecker checker;
  private ReserviorSubstanceSelector reserviorSubstanceSelector;
  
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

  public List<Droplet> compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    checker = new BioConstraintsChecker();
    reserviorSubstanceSelector = new ReserviorSubstanceSelector();
    
    reserviors = reserviorSubstanceSelector.select(assay, array);

    operationIdToExtra = new HashMap<>();

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.priority = operation.type == OperationType.Spawn ? 1 : 2;
      extra.done = false;
      extra.active = false;
      
      operationIdToExtra.put(operation.id, extra);
    }
    
    dropletIdGenerator = new UidGenerator();

    stalledOperations = new ArrayList<>();
    activatedOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();

    List<Operation> spawnOperations = assay.getOperations(OperationType.Spawn);
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
          Operation successor = stalled.outputs[0];
          
          List<Point> reservedSpawns = new ArrayList<>();
          
          boolean canParallelSpawn = true;
          for (Operation input : successor.inputs) {
            if (input.type == OperationType.Spawn) {
              
              // getDropletSpawn
              Point spawn = null;
              {
                outer: for (Reservior reservior : reserviors) {
                  if (!reservior.substance.equals(input.substance)) continue;
                  
                  for (Droplet droplet : runningDroplets) {
                    if (!checker.satifiesConstraints(reservior.position, droplet.at, droplet.to)) {
                      continue outer;
                    }
                  }
                  
                  for (Point otherSpawn : reservedSpawns) {
                    if (!checker.satisfiesSpacingConstraint(reservior.position, otherSpawn)) {
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

            for (Operation input : successor.inputs) {
              
              if (input.type == OperationType.Spawn) {
                Point spawn = reservedSpawns.remove(0);

                runningOperations.add(input);
                
                Droplet droplet = new Droplet();
                droplet.route.start = timestamp;
                droplet.to = spawn;
                droplet.id = dropletIdGenerator.getId();
                
                runningDroplets.add(droplet);
                
                OperationExtra extra = operationIdToExtra.get(input.id);
                extra.running = true;
                
                int forwardIndex = ArrayUtils.getFirstEmptySlotIndex(input.forwarding);
                input.forwarding[forwardIndex] = droplet;
              }
            }
          }
          
        } else {
          it.remove();
          
          runningOperations.add(stalled);
          stalledExtra.running = true;

          for (Operation input : stalled.inputs) {
            OperationExtra inputExtra = operationIdToExtra.get(input.id);
            
            Droplet forwardedDroplet = input.forwarding[inputExtra.forwardIndex];
            inputExtra.forwardIndex += 1;

            forwardedDroplet.operation = stalled;
            
            int manipulatingIndex = ArrayUtils.getFirstEmptySlotIndex(stalled.manipulating);
            stalled.manipulating[manipulatingIndex] = forwardedDroplet;
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
        if (operation.type == OperationType.Spawn) {
          // spawn location is already selected at this point. Do nothing.
          
        } else if (operation.type == OperationType.Merge) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];

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
            
            Droplet mergedDroplet = new Droplet();
            mergedDroplet.route.start = timestamp;
            mergedDroplet.to = newPosition1.copy();
            mergedDroplet.id = dropletIdGenerator.getId();
            
            int forwardIndex = ArrayUtils.getFirstEmptySlotIndex(operation.forwarding);
            operation.forwarding[forwardIndex] = mergedDroplet;
            
            runningDroplets.add(mergedDroplet);
          }

        } else if (operation.type == OperationType.Split) {
          Droplet droplet = operation.manipulating[0];

          boolean horizontalSplit = true;
          
          Point at = droplet.at;
          
          Point left = new Point(-1, 0).add(at);
          Point right = new Point(1, 0).add(at);
          
          if (!inside(left.x, left.y, array.width, array.height)) horizontalSplit = false;
          if (!inside(right.x, right.y, array.width, array.height)) horizontalSplit = false;
          
          for (Droplet other : runningDroplets) {
            if (other.id == droplet.id) continue;
            
            Point otherAt = other.at;
            
            boolean ok1 = checker.satifiesConstraints(at, left, otherAt, other.to);
            boolean ok2 = checker.satifiesConstraints(at, right, otherAt, other.to);
            
            if (!ok1 || !ok2) horizontalSplit = false;
          }

          boolean verticalSplit = true;
          
          Point up = new Point(0, 1).add(at);
          Point down = new Point(0, -1).add(at);
          
          if (!inside(up.x, up.y, array.width, array.height)) verticalSplit = false;
          if (!inside(down.x, down.y, array.width, array.height)) verticalSplit = false;
          
          for (Droplet other : runningDroplets) {
            if (other.id == droplet.id) continue;
            
            boolean ok1 = checker.satifiesConstraints(droplet.at, up, other.at, other.to);
            boolean ok2 = checker.satifiesConstraints(droplet.at, down, other.at, other.to);
            
            if (!ok1 || !ok2) verticalSplit = false;
          }

          Point to1 = verticalSplit ? up : left;
          Point to2 = verticalSplit ? down : right;
          
          boolean split = verticalSplit || horizontalSplit;
          if (split) {
            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            Droplet s1 = new Droplet();
            s1.route.start = timestamp;
            s1.to = to1;
            s1.id = dropletIdGenerator.getId();
            
            Droplet s2 = new Droplet();
            s2.route.start = timestamp;
            s2.to = to2;
            s2.id = dropletIdGenerator.getId();
            
            runningDroplets.add(s1);
            runningDroplets.add(s2);
            
            int forwardIndex1 = ArrayUtils.getFirstEmptySlotIndex(operation.forwarding);
            operation.forwarding[forwardIndex1] = s1;
            
            int forwardIndex2 = ArrayUtils.getFirstEmptySlotIndex(operation.forwarding);
            operation.forwarding[forwardIndex2] = s2;
            
          } else {
            // move somewhere, where it can split.
            Point move = getBestSplitMove(droplet, runningDroplets, array);
            if (move == null) throw new IllegalStateException("broken!");
            
            droplet.to = new Point(droplet.at).add(move);
          }
        } else if (operation.type == OperationType.Mix) {
          Droplet droplet = operation.manipulating[0];
          
          Point move = getBestMixMove(droplet, percentages, array);
          droplet.to = new Point(droplet.at).add(move);
          
          // @TODO: fix Mix. When is the Operation Mix setting forwarding? I don't think it ever does! 
          // I should figure out when forwarding is set (droplet created), before or after making the move.
          
        } else {
          throw new IllegalStateException("unsupported operation!");
        }
      }

      // ====================

      for (Operation operation : runningOperations) {
        if (operation.type != OperationType.Mix) continue;
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        Droplet droplet = operation.manipulating[0];

        Point previousMove = getPreviousMove(droplet);
        Point move = droplet.to.copy().sub(droplet.at);
        
        float mixing = percentages.getMixingPercentage(move, previousMove);
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
          Droplet droplet = operation.forwarding[0];
          if (droplet.at != null) extra.done = true;
          
        } else if (operation.type == OperationType.Merge) {
          extra.done = ArrayUtils.countOccupiedSlots(operation.forwarding) == 1;
        } else if (operation.type == OperationType.Split) {
          extra.done = ArrayUtils.countOccupiedSlots(operation.forwarding) == 2;
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
    
    List<Droplet> result = new ArrayList<>();
    result.addAll(retiredDroplets);

    return result;
  }
  
  private Point getBestMixMove(Droplet droplet, MixingPercentages percentages, BioArray array) {
    List<Point> validMoves = getValidMoves(droplet, null, timestamp, runningDroplets, array);
    Point prevMove = getPreviousMove(droplet);
    
    float bestPercentage = Float.MIN_VALUE;
    Point bestMove = null;
    
    for (Point move : validMoves) {
      float percentage = percentages.getMixingPercentage(move, prevMove);
      
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
        
        if (!checker.satifiesConstraints(droplet.at, next, other.at, other.to)) continue outer;
      }
      
      if (mergeSibling != null) {
        if (!checker.satisfiesCompanionConstraints(droplet.at, next, mergeSibling.at, mergeSibling.to)) continue;
      }
      
      validMoves.add(move);
    }
    
    return validMoves;
  }

  private Point getBestMergeMove(Droplet droplet, Droplet toMerge, List<Droplet> droplets, BioArray array) {
    List<Point> validMoves = getValidMoves(droplet, toMerge, timestamp, droplets, array);
    
    Point best = null;
    int shortestDistance = Integer.MAX_VALUE;

    Point at = droplet.at;
    Point target = (toMerge.to == null) ? toMerge.at : toMerge.to;

    for (Point move : validMoves) {
      Point next = new Point(at).add(move);

      int distance = MathUtils.getManhattenDistance(next.x, next.y, target.x, target.y);

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
    Point to = new Point();
    for (Point move : validMoves) {
      to.set(droplet.at).add(move);

      int distance1 = MathUtils.getManhattenDistance(to.x, to.y, 0, 0);
      int distance2 = MathUtils.getManhattenDistance(to.x, to.y, array.width - 1, 0);
      int distance3 = MathUtils.getManhattenDistance(to.x, to.y, 0, array.height - 1);
      int distance4 = MathUtils.getManhattenDistance(to.x, to.y, array.width - 1, array.height - 1);

      int minimumDistance = Math.min(Math.min(distance1, distance2), Math.min(distance3, distance4));
      
      if (minimumDistance >= longestDistance) {
        longestDistance = minimumDistance;
        bestMove = move;
      }
    }

    return bestMove;
  }

  private boolean inside(int x, int y, int width, int height) {
    return x >= 0 && x <= width - 1 && y >= 0 && y <= height - 1;
  }
}

class OperationExtra { // algorithm specific
  public int priority;

  public boolean active;
  public boolean running;
  public boolean done;
  
  public float mixingPercentage;  // only used for mixing operations.

  public int forwardIndex;
}


/*
class GreedyOperation extends Operation {
  public List<Integer> dropletId = new ArrayList<>();

  public int priority;

  public boolean active;
  public boolean running;
  public boolean done;
  
  public float mixingPercentage;  // only used for mixing operations.
}
*/