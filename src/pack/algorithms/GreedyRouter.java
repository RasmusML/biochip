package pack.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import engine.math.MathUtils;
import pack.algorithms.components.BioConstraintsChecker;
import pack.algorithms.components.MixingPercentages;
import pack.algorithms.components.ModuleManager;
import pack.algorithms.components.MoveFinder;
import pack.algorithms.components.RandomIndexSelector;
import pack.algorithms.components.RandomUtil;
import pack.algorithms.components.ReservoirManager;
import pack.algorithms.components.SubstanceToReservoirAssigner;
import pack.algorithms.components.UidGenerator;
import pack.helpers.Assert;
import pack.helpers.Logger;

public class GreedyRouter {

  private BioConstraintsChecker checker;
  private RandomIndexSelector indexSelector;
  private MoveFinder moveFinder;
  
  private ReservoirManager reservoirManager;

  private List<Operation> readyOperations;
  private List<Operation> runningOperations;

  private int aliveOperationsCount;

  private List<Droplet> runningDroplets;
  private List<Droplet> retiredDroplets;
  private List<Droplet> detouringDroplets;

  private Map<Integer, OperationExtra> operationIdToExtra;

  private UidGenerator dropletIdGenerator;

  private int maxIterations;
  
  private ModuleManager moduleManager;
  
  int timestamp;
  
  /*
   * The algorithm is based on the following papers with multiple additions, such as which droplets to dispense.
   * 
   * Greedy Randomized Adaptive Search Procedure:
   * http://www2.compute.dtu.dk/~paupo/publications/Maftei2012aa-Routing-based%20Synthesis%20of%20Dig-Design%20Automation%20for%20Embedded.pdf
   * http://www2.compute.dtu.dk/~paupo/publications/Windh2016aa-Performance%20Improvements%20and%20C-IEEE%20Transactions%20on%20Computer-.pdf
   * 
   */

  public RoutingResult compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    checker = new BioConstraintsChecker();
    indexSelector = new RandomIndexSelector();
    moveFinder = new MoveFinder(checker);

    SubstanceToReservoirAssigner s2rAssigner = new SubstanceToReservoirAssigner();
    List<Reservoir> reservoirs = s2rAssigner.assign(assay, array);
    reservoirManager = new ReservoirManager(reservoirs, checker);
    
    moduleManager = new ModuleManager(array.catalog);
    
    maxIterations = 1000;

    operationIdToExtra = new HashMap<>();

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.priority = operation.type == OperationType.Mix ? 2 : 1;
      extra.done = false;
      extra.active = false;

      operationIdToExtra.put(operation.id, extra);
    }

    dropletIdGenerator = new UidGenerator();

    readyOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();
    detouringDroplets = new ArrayList<>();

    List<Operation> dispenseOperations = assay.getOperations(OperationType.Dispense);
    readyOperations.addAll(dispenseOperations);

    aliveOperationsCount += readyOperations.size();

    timestamp = 0;

    boolean earlyTerminated = false;

    while (true) {

      for (Iterator<Operation> it = readyOperations.iterator(); it.hasNext();) {
        Operation stalled = it.next();
        OperationExtra stalledExtra = operationIdToExtra.get(stalled.id);

        // the stalled operation is already processed, which can happen if a "dispense"
        // operation is sibling to another "dispense" operation. So just cleanup this
        // "dispense" operation.
        if (stalledExtra.running) {
          it.remove();
          continue;
        }

        if (stalled.type == OperationType.Dispense) {
          Operation successor = stalled.outputs[0];

          Reservoir reserved = reservoirManager.reserve(stalled.substance, runningDroplets, timestamp);
          
          if (reserved != null) {
            
            if (successor.type == OperationType.Merge) {
              Operation predecessor0 = successor.inputs[0];
              Operation predecessor1 = successor.inputs[1];

              Operation sibling = (predecessor0.id == stalled.id) ? predecessor1 : predecessor0;
              if (sibling.type == OperationType.Dispense) {
                Reservoir siblingReservoir = reservoirManager.reserve(sibling.substance, runningDroplets, timestamp);

                if (siblingReservoir == null) {
                  if (sibling.substance.equals(stalled.substance)) {
                    int count = reservoirManager.countReservoirsContainingSubstance(sibling.substance);

                    if (count == 1) {
                      it.remove();
                      dispenseDroplet(stalled, reserved.position.copy());
                    }
                  }
                } else {
                  it.remove();

                  dispenseDroplet(stalled, reserved.position.copy());
                  dispenseDroplet(sibling, siblingReservoir.position.copy());
                }

              } else {
                OperationExtra siblingExtra = operationIdToExtra.get(sibling.id);
                if (siblingExtra.done) {
                  it.remove();
                  dispenseDroplet(stalled, reserved.position.copy());
                }
              }
              
            } else {
              it.remove();
              dispenseDroplet(stalled, reserved.position.copy());
            }
          }
          
          reservoirManager.consumeReservations();

        } else {
          it.remove();

          runningOperations.add(stalled);
          stalledExtra.running = true;
          
          if (stalled.type == OperationType.Module)  {
            stalledExtra.module = moduleManager.allocate(stalled.module);
          }

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
      runningOperations.sort((o1, o2) -> {
        OperationExtra e1 = operationIdToExtra.get(o1.id);
        OperationExtra e2 = operationIdToExtra.get(o2.id);
        return e1.priority - e2.priority;
      });
      
      for (Operation operation : runningOperations) {
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (operation.type == OperationType.Dispense) {
          // Dispense location is already selected at this point. Do nothing.
          
          Droplet droplet = operation.forwarding[0];
          Point to = droplet.route.getPosition(timestamp);
          Assert.that(to != null);

          extra.done = true;

        } else if (operation.type == OperationType.Merge) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];

          Move move0 = getMergeMove(droplet0, droplet1, runningDroplets, array);
          if (move0 == null) continue;
          
          Point at0 = droplet0.route.getPosition(timestamp - 1);
          Point to0 = at0.copy().add(move0.x, move0.y);
          droplet0.route.path.add(to0);

          Move move1 = getMergeMove(droplet1, droplet0, runningDroplets, array);
          if (move1 == null) continue;

          Point at1 = droplet1.route.getPosition(timestamp - 1);
          Point to1 = at1.copy().add(move1.x, move1.y);
          droplet1.route.path.add(to1);
          
          boolean merged = to0.x == to1.x && to0.y == to1.y;
          if (merged) {
            extra.done = true;

            Droplet mergedDroplet = createDroplet(to0);

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

          if (canSplit(droplet, Orientation.Vertical, runningDroplets, array)) {
            extra.done = true;

            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);

            Point at = droplet.route.getPosition(timestamp - 1);

            Point to1 = at.copy().add(Move.Up.x, Move.Up.y);
            Point to2 = at.copy().add(Move.Down.x, Move.Down.y);
            
            Droplet s1 = createDroplet(to1);
            Droplet s2 = createDroplet(to2);

            runningDroplets.add(s1);
            runningDroplets.add(s2);
            
            operation.forwarding[0] = s1;
            operation.forwarding[1] = s2;
            
          } else if (canSplit(droplet, Orientation.Horizontal, runningDroplets, array)) {
            extra.done = true;

            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            Point at = droplet.route.getPosition(timestamp - 1);

            Point to1 = at.copy().add(Move.Left.x, Move.Left.y);
            Point to2 = at.copy().add(Move.Right.x, Move.Right.y);
            
            Droplet s1 = createDroplet(to1);
            Droplet s2 = createDroplet(to2);

            runningDroplets.add(s1);
            runningDroplets.add(s2);
            
            operation.forwarding[0] = s1;
            operation.forwarding[1] = s2;
          } else {
            // move somewhere, where it can split.
            Move move = getSplitMove(droplet, runningDroplets, array);
            if (move == null) continue;

            Point at = droplet.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);
            droplet.route.path.add(to);
          }
          
        } else if (operation.type == OperationType.Mix) {
          Droplet droplet = operation.manipulating[0];

          Point at = droplet.route.getPosition(timestamp - 1);
          Move move = getMixMove(droplet, percentages, array);
          if (move == null) continue;
          
          Move previousMove = droplet.route.getMove(timestamp - 2);
          float mixing = percentages.getMixingPercentage(move, previousMove);

          Point to = at.copy().add(move.x, move.y);
          
          extra.mixingPercentage += mixing;
          if (extra.mixingPercentage >= 100f) {
            extra.mixingPercentage = 100;
            extra.done = true;

            Droplet forward = createDroplet(to);

            operation.forwarding[0] = forward;

            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);

            runningDroplets.add(forward);
            
          } else {
            droplet.route.path.add(to);
          }
        } else if (operation.type == OperationType.Module) {
          Droplet droplet = operation.manipulating[0];
          
          Module module = extra.module;
          
          Point at = droplet.route.getPosition(timestamp - 1);
          Move move = getModuleMove(droplet, runningDroplets, module, array);
          if (move == null) continue;
          
          Point to = at.copy().add(move.x, move.y);
          
          if (within(to.x, to.y, module.position.x, module.position.y, module.width, module.height)) {
            extra.currentDurationInTimesteps += 1;
          }
          
          if (extra.currentDurationInTimesteps >= module.duration) {
            extra.done = true;
            
            runningDroplets.remove(droplet);
            retiredDroplets.add(droplet);
            
            Droplet forward = createDroplet(to);
            operation.forwarding[0] = forward;
            runningDroplets.add(forward);
            
          } else {
            droplet.route.path.add(to);
          }
          
        } else {
          throw new IllegalStateException("unsupported operation!");
        }
      }
      
      for (Droplet droplet : detouringDroplets) {
        Point at = droplet.route.getPosition(timestamp - 1);
        
        Move move = getDetourMove(droplet, runningDroplets, array);
        Point to = at.copy().add(move.x, move.y);

        droplet.route.path.add(to);
      }
      
      detouringDroplets.clear();

      // select a move to droplets which did not get a move during the operation-phase.
      // This can happen, if a droplet is done with its operation or if a droplet should detour.
      for (Droplet droplet : runningDroplets) {
        Point to = droplet.route.getPosition(timestamp);
        if (to != null) continue;
        
        List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
        
        Point at = droplet.route.getPosition(timestamp - 1);
        if (validMoves.size() > 0) {
          int selectedIndex = indexSelector.selectUniformly(validMoves.size() - 1);
          
          Move move = validMoves.get(selectedIndex);
          to = at.copy().add(move.x, move.y);
          droplet.route.path.add(to);
        } else {
          Move move = getDetourMove(droplet, runningDroplets, array);
          to = at.copy().add(move.x, move.y);

          droplet.route.path.add(to);
        }
      }
      
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
              readyOperations.add(descendant);
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
    result.reservoirs.addAll(reservoirManager.getReservoirs());
    result.executionTime = timestamp;
    
    List<Module> modulePlacements = moduleManager.getModules();
    result.modules.addAll(modulePlacements);

    return result;
  }
  
  private Move getDetourMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Point at = droplet.route.getPosition(timestamp - 1);
    
    List<Module> inUseModules = moduleManager.getInUseOrAlwaysLockedModules();
    
    Module module = null;
    for (Module other : inUseModules) {
      if (within(at.x, at.y, other.position.x, other.position.y, other.width, other.height)) {
        module = other;
        break;
      }
    }
    
    Assert.that(module != null);
    
    Move bestMove = null;
    float maxDistance = -1;
    
    Point to = new Point();
    
    // @TODO: probabilistic moves, it possible for deadlocks @create test which does deadlock!
    
    List<Move> moves = moveFinder.getValidMoves(droplet, module, timestamp, droplets, inUseModules, array);
    for (Move move : moves) {
      to.set(at).add(move.x, move.y);
      
      float mcx = module.position.x + module.width / 2f;
      float mcy = module.position.y + module.height / 2f;
      
      float distance = MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);
      if (distance > maxDistance) {
        maxDistance = distance;
        bestMove = move;
      }
    }
    
    return bestMove;
  }

  private boolean within(int px, int py, int x, int y, int width, int height) {
    return px <= x + width - 1 && px >= x && py <= y + height - 1 && py >= y;
  }

  private Droplet createDroplet(Point position) {
    Droplet droplet = new Droplet();
    droplet.route.start = timestamp;
    droplet.route.path.add(position);
    droplet.id = dropletIdGenerator.getId();
    return droplet;
  }

  private void dispenseDroplet(Operation operation, Point position) {
    OperationExtra extra = operationIdToExtra.get(operation.id);
    extra.running = true;
    
    runningOperations.add(operation);

    Droplet droplet = createDroplet(position);

    runningDroplets.add(droplet);

    operation.forwarding[0] = droplet;
  }

  private Move getMixMove(Droplet droplet, MixingPercentages percentages, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
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

  private boolean canSplit(Droplet droplet, Orientation orientation, List<Droplet> droplets, BioArray array) {
    List<Move> moves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleManager.getInUseOrAlwaysLockedModules(), array);

    if (orientation == Orientation.Vertical) {
      if (moves.contains(Move.Up) && moves.contains(Move.Down)) return true;
    } else if (orientation == Orientation.Horizontal) {
      if (moves.contains(Move.Left) && moves.contains(Move.Right)) return true;
    } else {
      throw new IllegalStateException("invalid orientation!");
    }
    
    return false;
  }

  private Move getMergeMove(Droplet droplet, Droplet toMerge, List<Droplet> droplets, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, toMerge, timestamp, droplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;
    
    Collections.shuffle(validMoves, RandomUtil.get());
    // if we use the manhattan distance, then reverse, turn directions yield the
                                     // same manhattan distance, meaning all moves are just as good. However, we only
                                     // select the 3 best moves, so if we don't shuffle, then the last one will
                                     // always be ignored (due to we always insert the moves in the same order).
    
    Point at = droplet.route.getPosition(timestamp - 1);

    Point toMergeAt = toMerge.route.getPosition(timestamp - 1);
    Point toMergeTo = toMerge.route.getPosition(timestamp);

    Point target = toMergeTo != null ? toMergeTo : toMergeAt;
    Point next = new Point();

    validMoves.sort((move1, move2) -> {
      next.set(at).add(move1.x, move1.y);
      // int distance1 = (int) MathUtils.distance(next.x - target.x, next.y -
      // target.y);
      int distance1 = (int) MathUtils.getManhattanDistance(next.x, next.y, target.x, target.y);

      next.set(at).add(move2.x, move2.y);
      // int distance2 = (int) MathUtils.distance(next.x - target.x, next.y -
      // target.y);
      int distance2 = (int) MathUtils.getManhattanDistance(next.x, next.y, target.x, target.y);

      return distance1 - distance2;
    });

    float[] allWeights = { 50f, 33.3f, 16.6f };

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(allWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);

    return validMoves.get(bestMoveIndex);
    
  }
  
  private Move getModuleMove(Droplet droplet, List<Droplet> droplets, Module module, BioArray array) {
    Move bestMove = null;
    float bestMoveDistance = Float.MAX_VALUE;
    
    List<Move> validMoves = moveFinder.getValidMoves(droplet, module, timestamp, droplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
    Point at = droplet.route.getPosition(timestamp - 1);
    
    Point to = new Point();
    for (Move move : validMoves) {
      to.set(at).add(move.x, move.y);
      
      float distance = MathUtils.distanceToRectangle(to.x, to.y, module.position.x, module.position.y, module.width, module.height);
      if (distance < bestMoveDistance) {
        bestMoveDistance = distance;
        bestMove = move;
      }
    }
    
    return bestMove;
  }
  
  
  private Move getSplitMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Move bestMove = null;
    int longestDistance = 0;

    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleManager.getInUseOrAlwaysLockedModules(), array);

    Point at = droplet.route.getPosition(timestamp - 1);

    // select move which is furthest away from wall corner.
    Point to = new Point();
    for (Move move : validMoves) {
      to.set(at).add(move.x, move.y);

      int distance1 = (int )MathUtils.getManhattanDistance(to.x, to.y, 0, 0);
      int distance2 = (int) MathUtils.getManhattanDistance(to.x, to.y, array.width - 1, 0);
      int distance3 = (int) MathUtils.getManhattanDistance(to.x, to.y, 0, array.height - 1);
      int distance4 = (int) MathUtils.getManhattanDistance(to.x, to.y, array.width - 1, array.height - 1);

      int minimumDistance = Math.min(Math.min(distance1, distance2), Math.min(distance3, distance4));

      if (minimumDistance >= longestDistance) {
        longestDistance = minimumDistance;
        bestMove = move;
      }
    }

    return bestMove;
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
  // public int stepsSinceLastProgress;
  // public float mostProgress;

  public float mixingPercentage; // only used for mixing operations.
//  public float currentTemperature; // only used for heating operations.
  
  public int currentDurationInTimesteps;
  public Module module;
}


/*
 * class GreedyOperation extends Operation { public List<Integer> dropletId =
 * new ArrayList<>();
 * 
 * public int priority;
 * 
 * public boolean active; public boolean running; public boolean done;
 * 
 * public float mixingPercentage; // only used for mixing operations. }
 */
