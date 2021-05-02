package pack.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.jvm.format.Merge;

import engine.math.MathUtils;
import pack.algorithms.components.BioConstraintsChecker;
import pack.algorithms.components.MixingPercentages;
import pack.algorithms.components.MoveFinder;
import pack.algorithms.components.RandomIndexSelector;
import pack.algorithms.components.ReserviorSubstanceSelector;
import pack.algorithms.components.UidGenerator;

public class GreedyRouter {
  
  private BioConstraintsChecker checker;
  private ReserviorSubstanceSelector reserviorSubstanceSelector;
  private RandomIndexSelector indexSelector;
  private MoveFinder moveFinder;
  
  private List<Operation> readyOperations;
  private List<Operation> activatedOperations;
  private List<Operation> runningOperations;
  
  private int aliveOperationsCount;

  private List<Droplet> runningDroplets;
  private List<Droplet> retiredDroplets;

  private List<Reservior> reserviors;

  private Map<Integer, OperationExtra> operationIdToExtra;
  
  private UidGenerator dropletIdGenerator;
  
  private int maxIterations;
  
  int timestamp;

  public RoutingResult compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    checker = new BioConstraintsChecker();
    reserviorSubstanceSelector = new ReserviorSubstanceSelector();
    indexSelector = new RandomIndexSelector();
    moveFinder = new MoveFinder(checker);
    
    reserviors = reserviorSubstanceSelector.select(assay, array);
    
    maxIterations = 1000;

    operationIdToExtra = new HashMap<>();

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.priority = 1;
      extra.done = false;
      extra.active = false;
      
      operationIdToExtra.put(operation.id, extra);
    }
    
    dropletIdGenerator = new UidGenerator();

    readyOperations = new ArrayList<>();
    activatedOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();

    List<Operation> dispenseOperations = assay.getOperations(OperationType.Dispense);
    activatedOperations.addAll(dispenseOperations);
    
    aliveOperationsCount += activatedOperations.size();

    timestamp = 0;
    
    boolean earlyTerminated = false;
    
    while (true) {

      // ====================
      //     Custom Layer
      // ====================
      
      readyOperations.addAll(activatedOperations);

      for (Iterator<Operation> it = readyOperations.iterator(); it.hasNext();) {
        Operation stalled = it.next();
        OperationExtra stalledExtra = operationIdToExtra.get(stalled.id);
        
        // the stalled operation is already processed, which can happen if a "spawn" operation is sibling to another "spawn" operation. So just cleanup this "spawn" operation. 
        if (stalledExtra.running) {
          it.remove();
          continue;
        }
        
        if (stalled.type == OperationType.Dispense) {
          Operation successor = stalled.outputs[0];
          
          // @TODO: ReserviorDelegator
          Reservior reserved = null;
          outer: for (Reservior reservior : reserviors) {
            
            if (reservior.substance.equals(stalled.substance)) {
              for (Droplet droplet : runningDroplets) {
                Point at = droplet.route.getPosition(timestamp - 1);
                Point to = droplet.route.getPosition(timestamp);
                
                if (!checker.satifiesConstraints(reservior.position, at, to)) {
                  continue outer;
                }
              }

              reserved = reservior;
              break;
            }
          }
          
          if (reserved == null) continue; // don't spawn

          if (successor.type == OperationType.Merge) {
            Operation predecessor0 = successor.inputs[0];
            Operation predecessor1 = successor.inputs[1];
            
            Operation sibling = (predecessor0.id == stalled.id) ?  predecessor1 : predecessor0;
            if (sibling.type == OperationType.Dispense) {
              Reservior siblingReservior = null;
              
              outer: for (Reservior reservior : reserviors) {
              if (reservior == reserved) continue;
              
              if (reservior.substance.equals(sibling.substance)) {
                for (Droplet droplet : runningDroplets) {
                  Point at = droplet.route.getPosition(timestamp - 1);
                  Point to = droplet.route.getPosition(timestamp);
                  
                  if (!checker.satifiesConstraints(reservior.position, at, to)) continue outer;
                }

                siblingReservior = reservior;
                break;
                }
              }
              
              if (siblingReservior == null) continue; // don't spawn

              // spawn sibling             runningOperations.add(sibling);                          Droplet droplet = new Droplet();             droplet.route.start = timestamp;             droplet.route.path.add(siblingReservior.position.copy());             droplet.id = dropletIdGenerator.getId();                          runningDroplets.add(droplet);                          OperationExtra extra = operationIdToExtra.get(sibling.id);             extra.running = true;                          sibling.forwarding[0] = droplet;
              
            } else {
              OperationExtra siblingExtra = operationIdToExtra.get(sibling.id);
              if (!siblingExtra.done) continue; // don't spawn
            
            }
          }
           
          it.remove();
                      // spawn it           runningOperations.add(stalled);                      Droplet droplet = new Droplet();           droplet.route.start = timestamp;           droplet.route.path.add(reserved.position.copy());           droplet.id = dropletIdGenerator.getId();                      runningDroplets.add(droplet);                     stalledExtra.running = true;                      stalled.forwarding[0] = droplet;
          
        } else {
          it.remove();
          
          runningOperations.add(stalled);
          stalledExtra.running = true;

          for (int i = 0; i < stalled.inputs.length; i++) {
            Operation input = stalled.inputs[i];
            OperationExtra inputExtra = operationIdToExtra.get(input.id);
            
            Droplet forwardedDroplet = input.forwarding[inputExtra.forwardIndex];
            inputExtra.forwardIndex += 1;

            forwardedDroplet.operation = stalled;
            
            stalled.manipulating[i] = forwardedDroplet;
          }
        }
      }
     
      // @TODO: remove this
      // this makes test 4 (PCR tree work). It is just pure luck, that the droplets do not block.
      readyOperations.sort((o1, o2) -> {
        OperationExtra e1 = operationIdToExtra.get(o1.id);
        OperationExtra e2 = operationIdToExtra.get(o2.id);
        return e1.priority - e2.priority;
      });

      // choose action
      for (Operation operation : runningOperations) {
        if (operation.type == OperationType.Dispense) {
          // Dispense location is already selected at this point. Do nothing.
          
        } else if (operation.type == OperationType.Merge) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];

          Move move0 = getBestMergeMove(droplet0, droplet1, runningDroplets, array);
          Point at0 = droplet0.route.getPosition(timestamp - 1);
          Point newPosition0 = at0.copy().add(move0.x, move0.y);
          droplet0.route.path.add(newPosition0);

          Move move1 = getBestMergeMove(droplet1, droplet0, runningDroplets, array);
          Point at1 = droplet1.route.getPosition(timestamp - 1);
          Point newPosition1 = at1.copy().add(move1.x, move1.y);
          droplet1.route.path.add(newPosition1);
          
        } else if (operation.type == OperationType.Split) {
          Droplet droplet = operation.manipulating[0];

          OperationExtra extra = operationIdToExtra.get(operation.id);
          if (canSplit(droplet, Orientation.Vertical, array)) {
            extra.split = Orientation.Vertical;
          } else if (canSplit(droplet, Orientation.Horizontal, array)) {
            extra.split = Orientation.Horizontal;
          } else {
            // move somewhere, where it can split.
            Move move = getBestSplitMove(droplet, runningDroplets, array);
            
            Point at = droplet.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);
            droplet.route.path.add(to);
          }
        } else if (operation.type == OperationType.Mix) {
          Droplet droplet = operation.manipulating[0];
          
          Point at = droplet.route.getPosition(timestamp - 1);
          Move move = getBestMixMove(droplet, percentages, array);
          Point to = at.copy().add(move.x, move.y);
          droplet.route.path.add(to);
          
        } else { 
          throw new IllegalStateException("unsupported operation!");
        }
      }
      
      // droplets which don't have a move will stay where they are. It is only the droplets which are completely done that it will occur to.
      for (Droplet droplet : runningDroplets) {
        Point to = droplet.route.getPosition(timestamp);
        
        if (to == null) {
          to = droplet.route.getPosition(timestamp - 1);
          droplet.route.path.add(to.copy());
        }
      }

      // ====================
      
      // check if operations are done and prepare output droplets.
      for (Operation operation : runningOperations) {
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (operation.type == OperationType.Dispense) {
          Droplet droplet = operation.forwarding[0];
          Point to = droplet.route.getPosition(timestamp);
          Assert.that(to != null);
          
          extra.done = true;

        } else if (operation.type == OperationType.Merge) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];
          
          Point to0 = droplet0.route.getPosition(timestamp);
          Point to1 = droplet1.route.getPosition(timestamp);
          
          boolean merged = to0.x == to1.x && to0.y == to1.y;
          if (merged) {
            extra.done = true;
            
            Point to = to0.copy();
            
            Droplet mergedDroplet = new Droplet();
            mergedDroplet.route.start = timestamp;
            mergedDroplet.route.path.add(to);
            mergedDroplet.id = dropletIdGenerator.getId();
            
            operation.forwarding[0] = mergedDroplet;
            
            runningDroplets.add(mergedDroplet);
            
            droplet0.route.path.remove(droplet0.route.path.size() - 1);
            droplet1.route.path.remove(droplet1.route.path.size() - 1);
            
            runningDroplets.remove(droplet0);
            runningDroplets.remove(droplet1);

            retiredDroplets.add(droplet0);
            retiredDroplets.add(droplet1);
          }

        } else if (operation.type == OperationType.Split) {
          Droplet droplet = operation.manipulating[0];
          
          if (extra.split != null) {
            extra.done = true;
            
            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            Move move1, move2;
            if (extra.split == Orientation.Vertical) {
              move1 = Move.Up;
              move2 = Move.Down;
            } else if (extra.split == Orientation.Horizontal) {
              move1 = Move.Left;
              move2 = Move.Right;
            } else {
              throw new IllegalStateException("invalid orientation!");
            }
            
            Point at = droplet.route.getPosition(timestamp - 1);
            
            Point to1 = at.copy().add(move1.x, move1.y);
            Point to2 = at.copy().add(move2.x, move2.y);
            
            Droplet s1 = new Droplet();
            s1.route.start = timestamp;
            s1.route.path.add(to1);
            s1.id = dropletIdGenerator.getId();
            
            Droplet s2 = new Droplet();
            s2.route.start = timestamp;
            s2.route.path.add(to2);
            s2.id = dropletIdGenerator.getId();
            
            runningDroplets.add(s1);
            runningDroplets.add(s2);
            
            operation.forwarding[0] = s1;
            operation.forwarding[1] = s2;
          }
          
        } else if (operation.type == OperationType.Mix) {
          Droplet droplet = operation.manipulating[0];
          
          Move previousMove = droplet.route.getMove(timestamp - 2);
          Move currentMove = droplet.route.getMove(timestamp - 1);
          float mixing = percentages.getMixingPercentage(currentMove, previousMove);
          
          extra.mixingPercentage += mixing;
          if (extra.mixingPercentage >= 100f) {
            extra.mixingPercentage = 100;
            extra.done = true;
            
            Point to = droplet.route.path.remove(droplet.route.path.size() - 1);
            
            Droplet forward = new Droplet();
            forward.route.start = timestamp;
            forward.id = dropletIdGenerator.getId();
            forward.route.path.add(to);

            operation.forwarding[0] = forward;
            
            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            runningDroplets.add(forward);
          }
        }
      }
      
      // cleanup done operations and queue descended operations
      activatedOperations.clear();

      for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (extra.done) {
          it.remove();
          aliveOperationsCount -= 1;

          Logger.log("completed %d (%s)\n", operation.id, operation.type);

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

      timestamp += 1;
      
      // early terminate.
      if (timestamp > maxIterations) {
        earlyTerminated = true;
        
        break; 
      }
      
      if (aliveOperationsCount == 0) break;

    }
    
    retiredDroplets.addAll(runningDroplets);
    runningDroplets.clear();
    
    RoutingResult result = new RoutingResult();
    result.completed = !earlyTerminated;
    result.droplets.addAll(retiredDroplets);
    result.reserviors.addAll(reserviors);
    result.executionTime = timestamp;

    return result;
  }
  
  private Move getBestMixMove(Droplet droplet, MixingPercentages percentages, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, null, timestamp, runningDroplets, array);
    Move prevMove = droplet.route.getMove(timestamp - 2);
    
    float bestPercentage = Float.NEGATIVE_INFINITY;
    Move bestMove = null;
    
    for (Move move : validMoves) {
      float percentage = percentages.getMixingPercentage(move, prevMove);
      
      if (percentage > bestPercentage) {
        bestPercentage = percentage;
        bestMove = move;
      }
    }
    
    return bestMove;
  }
  
  private boolean canSplit(Droplet droplet, Orientation orientation, BioArray array) {
    Point at = droplet.route.getPosition(timestamp - 1);
    
    Point to1 = at.copy();
    Point to2 = at.copy();
    
    if (orientation == Orientation.Vertical) {
      to1.add(Move.Up.x, Move.Up.y);
      to2.add(Move.Down.x, Move.Down.y);
    } else if (orientation == Orientation.Horizontal) {
      to1.add(Move.Left.x, Move.Left.y);
      to2.add(Move.Right.x, Move.Right.y);
    } else {
      throw new IllegalStateException("invalid orientation!");
    }
    
    if (!inside(to1.x, to1.y, array.width, array.height)) return false;
    if (!inside(to2.x, to2.y, array.width, array.height)) return false;
    
    for (Droplet other : runningDroplets) {
      if (other.id == droplet.id) continue;
      
      Point otherAt = other.route.getPosition(timestamp - 1);
      Point otherTo = other.route.getPosition(timestamp);
      
      boolean ok1 = checker.satifiesConstraints(at, to1, otherAt, otherTo);
      boolean ok2 = checker.satifiesConstraints(at, to2, otherAt, otherTo);
      
      if (!ok1 || !ok2) return false;
    }

    return true;
  }
  
  private Move getBestMergeMove(Droplet droplet, Droplet toMerge, List<Droplet> droplets, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, toMerge, timestamp, droplets, array);
    Collections.shuffle(validMoves);  // if we use the manhattan distance, then reverse, turn directions yield the same manhattan distance, meaning all moves are just as good. However, we only select the 3 best moves, so if we don't shuffle, then the last one will always be ignored (due to we always insert the moves in the same order).
    
    Point at = droplet.route.getPosition(timestamp - 1);
    
    Point toMergeAt = toMerge.route.getPosition(timestamp - 1);
    Point toMergeTo = toMerge.route.getPosition(timestamp);
    
    Point target = toMergeTo != null ? toMergeTo : toMergeAt;
    Point next = new Point();
    
    validMoves.sort((move1, move2) -> {
      next.set(at).add(move1.x, move1.y);
      //int distance1 = (int) MathUtils.distance(next.x - target.x, next.y - target.y);
      int distance1 = MathUtils.getManhattanDistance(next.x, next.y, target.x, target.y);
      
      next.set(at).add(move2.x, move2.y);
      //int distance2 = (int) MathUtils.distance(next.x - target.x, next.y - target.y);
      int distance2 = MathUtils.getManhattanDistance(next.x, next.y, target.x, target.y);
      
      return distance1 - distance2;
    });
    
    float[] allWeights = {50f, 33.3f, 16.6f};

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());
    
    float[] weights = new float[candidateSize];
    System.arraycopy(allWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);
    
    return validMoves.get(bestMoveIndex);
  }
  
  private Move getBestSplitMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Move bestMove = null;
    int longestDistance = 0;

    List<Move> validMoves = moveFinder.getValidMoves(droplet, null, timestamp, droplets, array);
    
    Point at = droplet.route.getPosition(timestamp - 1);

    // select move which is furthest away from wall corner.
    Point to = new Point();
    for (Move move : validMoves) {
      to.set(at).add(move.x, move.y);

      int distance1 = MathUtils.getManhattanDistance(to.x, to.y, 0, 0);
      int distance2 = MathUtils.getManhattanDistance(to.x, to.y, array.width - 1, 0);
      int distance3 = MathUtils.getManhattanDistance(to.x, to.y, 0, array.height - 1);
      int distance4 = MathUtils.getManhattanDistance(to.x, to.y, array.width - 1, array.height - 1);

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

// OperationAttachment
class OperationExtra {
  public int priority;

  public boolean active;
  public boolean running;
  public boolean done;
  
  public int forwardIndex;
  
  // @TODO
  //public int stepsSinceLastProgress;
  //public float mostProgress;
  
  public float mixingPercentage;  // only used for mixing operations.
  public float currentTemperature;  // only used for heating operations.
  public Orientation split; // only used for splitting operations
}

class HeatingModule {
  public int width, height;
  public int duration;  // in timesteps for now.
}

class ModulePlacement {
  public HeatingModule module;
  public Point at;
  public int start, end;
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

