package pack.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import engine.math.MathUtils;
import pack.algorithms.components.ConstraintsChecker;
import pack.algorithms.components.MixingPercentages;
import pack.algorithms.components.ModuleAllocator;
import pack.algorithms.components.SingleCellMoveFinder;
import pack.algorithms.components.RandomIndexSelector;
import pack.algorithms.components.ReservoirManager;
import pack.algorithms.components.SubstanceToReservoirAssigner;
import pack.algorithms.components.UidGenerator;
import pack.helpers.Assert;
import pack.helpers.GeometryUtil;
import pack.helpers.Logger;
import pack.helpers.RandomUtil;

public class GreedyRouter implements Router {

  private ConstraintsChecker checker;
  private RandomIndexSelector indexSelector;
  private MoveFinder moveFinder;
  //private ModifiedAStarPathFinder pathFinder;
  
  private Prioritizer prioritizer;
  
  private ReservoirManager reservoirManager;
  private ModuleAllocator moduleAllocator;

  private List<Operation> readyOperations;
  private List<Operation> runningOperations;

  private List<Droplet> runningDroplets;
  private List<Droplet> retiredDroplets;
  private List<Droplet> detouringDroplets;

  private Map<Integer, OperationExtra> operationIdToExtra;

  private UidGenerator dropletIdGenerator;

  private int maxIterationsPerOperation;
  private int iteration;
  
  private int timestamp;
  
  private float[] probabilitiesA, probabilitiesB, probabilitiesC;
  
  /*
   * The algorithm is based on the following papers:
   * 
   * Greedy Randomized Adaptive Search Procedure:
   * http://www2.compute.dtu.dk/~paupo/publications/Maftei2012aa-Routing-based%20Synthesis%20of%20Dig-Design%20Automation%20for%20Embedded.pdf
   * 
   * Performance Improvements and Congestion Reduction for Routing-based Synthesis for Digital Microfluidic Biochips:
   * http://www2.compute.dtu.dk/~paupo/publications/Windh2016aa-Performance%20Improvements%20and%20C-IEEE%20Transactions%20on%20Computer-.pdf
   * 
   */

  @Override
  public RoutingResult compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    checker = new ConstraintsChecker();
    indexSelector = new RandomIndexSelector();
    moveFinder = new SingleCellMoveFinder(checker);

    moduleAllocator = new ModuleAllocator(array.catalog);

    SubstanceToReservoirAssigner s2rAssigner = new SubstanceToReservoirAssigner();
    s2rAssigner.assign(assay, array, moduleAllocator);
    reservoirManager = new ReservoirManager(moduleAllocator, checker);
    
    probabilitiesA = new float[] {85f, 10f, 5f};
    probabilitiesB = new float[] {50f, 33f, 17f};
    probabilitiesC = new float[] {34f, 33f, 33f};
    
    prioritizer = new ChainPrioritizer();
    
    //pathFinder = new ModifiedAStarPathFinder();
    
    maxIterationsPerOperation = 500;  // if no operation terminates before iteration is this value, then we assume that no solution can be found.
    iteration = 0;

    operationIdToExtra = new HashMap<>();

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.priority = operation.name.equals(OperationType.mix) ? 2 : 1;
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

    List<Operation> dispenseOperations = assay.getOperations(OperationType.dispense);
    readyOperations.addAll(dispenseOperations);

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

        if (stalled.name.equals(OperationType.dispense)) {
          Operation successor = stalled.outputs[0];

          String substance = (String) stalled.attributes.get(Tags.substance);
          Module reserved = reservoirManager.reserve(substance, runningDroplets, timestamp);
          
          if (reserved != null) {
            
            if (successor != null && successor.name.equals(OperationType.merge)) {
              Operation predecessor0 = successor.inputs[0];
              Operation predecessor1 = successor.inputs[1];

              Operation sibling = (predecessor0.id == stalled.id) ? predecessor1 : predecessor0;
              if (sibling.name.equals(OperationType.dispense)) {
                String siblingSubstance = (String) sibling.attributes.get(Tags.substance);
                
                Module siblingReservoir = reservoirManager.reserve(siblingSubstance, runningDroplets, timestamp);

                if (siblingReservoir == null) {
                  if (siblingSubstance.equals(substance)) {
                    int count = reservoirManager.countReservoirsContainingSubstance(siblingSubstance);

                    if (count == 1) {
                      it.remove();
                      dispenseDroplet(stalled, reserved);
                    }
                  }
                } else {
                  it.remove();

                  dispenseDroplet(stalled, reserved);
                  dispenseDroplet(sibling, siblingReservoir);
                }

              } else {
                OperationExtra siblingExtra = operationIdToExtra.get(sibling.id);
                if (siblingExtra.done) {
                  it.remove();
                  dispenseDroplet(stalled, reserved);
                }
              }
              
            } else {
              it.remove();
              dispenseDroplet(stalled, reserved);
            }
          }
          
          reservoirManager.consumeReservations();

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
          
          if (stalled.name.equals(OperationType.heating))  {
            float temperature = (float) stalled.attributes.get(Tags.temperature);
            
            Droplet droplet = stalled.manipulating[0];
            BoundingBox boundingBox = droplet.getBoundingBox();
            stalledExtra.module = moduleAllocator.allocate(OperationType.heating, boundingBox.width, boundingBox.height, new Tag(Tags.temperature, temperature));
            
            /*
            Droplet droplet = stalled.manipulating[0];
            
            Point target = stalledExtra.module.position;
            List<Point> path = pathFinder.search(droplet, target, stalledExtra.module, runningDroplets, array, moveFinder, moduleAllocator.getInUseOrAlwaysLockedModules(), timestamp - 1, 30);
            droplet.route.path.addAll(path);
            
            for (Point point : path) {
              System.out.printf(">>%s\n", point.toString());
            }
            */
          }
          
        }
      }

      runningOperations.sort((o1, o2) -> prioritizer.prioritize(o1, o2));
      
      for (Operation operation : runningOperations) {
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (operation.name.equals(OperationType.dispense)) {
          // the first move is a bit special, because the move was selected during the spawn above.
          // the first move is already "processed" so skip that.
          Droplet droplet = operation.manipulating[0];
          DropletUnit unit = droplet.units.get(0);
          Point to = unit.route.getPosition(timestamp);
          if (to != null) continue;

          extra.currentDurationInTimesteps += 1;
          
          Module dispenser = extra.module;
          if (extra.currentDurationInTimesteps >= dispenser.duration) {
            retire(droplet);
            Droplet forwarded = createForwardedDroplet(Move.None, droplet, droplet.area);
            
            moduleAllocator.free(dispenser);
            
            operation.forwarding[0] = forwarded;
            extra.done = true;
          } else {
            move(droplet, Move.None);
          }

        } else if (operation.name.equals(OperationType.merge)) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];

          Move move0 = getMergeMove(droplet0, droplet1, runningDroplets, array);
          if (move0 == null) continue;  // no move? detour!
          
          move(droplet0, move0);

          Move move1 = getMergeMove(droplet1, droplet0, runningDroplets, array);
          if (move1 == null) continue; // no move? detour!
          
          move(droplet1, move1);
          
          if (merged(droplet0, droplet1)) {
            extra.done = true;

            Droplet mergedDroplet = createMergedDroplet(droplet0, droplet1);
            operation.forwarding[0] = mergedDroplet;

            // undo move, it has been set on the merged droplet
            DropletUnit unit0 = droplet0.units.get(0);
            DropletUnit unit1 = droplet1.units.get(0);

            unit0.route.path.remove(unit0.route.path.size() - 1);
            unit1.route.path.remove(unit1.route.path.size() - 1);

            retire(droplet0);
            retire(droplet1);
          }

        } else if (operation.name.equals(OperationType.split)) {
          Droplet droplet = operation.manipulating[0];

          if (canSplit(droplet, Orientation.Vertical, runningDroplets, array)) {
            extra.done = true;

            retire(droplet);
            
            float area1 = droplet.area / 2f;
            float area2 = droplet.area - area1;
            
            // select the bottom droplet-units to go down
            Droplet d1 = createForwardedDroplet(Move.Down, droplet, area1);  // @TODO: fix droplet successor is overriden, so the visualization is not quit right.
            
            // select the top droplet-units to go up
            Droplet d2 = createForwardedDroplet(Move.Up, droplet, area2);
            
            operation.forwarding[0] = d1;
            operation.forwarding[1] = d2;
            
          } else if (canSplit(droplet, Orientation.Horizontal, runningDroplets, array)) {
            extra.done = true;

            retire(droplet);
            
            float area1 = droplet.area / 2f;
            float area2 = droplet.area - area1;

            // select the left droplet-units to go left
            Droplet d1 = createForwardedDroplet(Move.Left, droplet, area1);
            
            // select the right droplet-units to go right
            Droplet d2 = createForwardedDroplet(Move.Right, droplet, area2);
            
            operation.forwarding[0] = d1;
            operation.forwarding[1] = d2;
            
          } else {
            // move somewhere, where it can split.
            Move move = getSplitMove(droplet, runningDroplets, array);
            if (move == null) continue;
            
            move(droplet, move);
          }
          
        } else if (operation.name.equals(OperationType.mix)) {
          Droplet droplet = operation.manipulating[0];

          Move move = getMixMove(droplet, percentages, array);
          if (move == null) continue;
          
          Move previousMove = droplet.units.get(0).route.getMove(timestamp - 2);  // @Cleanup
          float mixing = percentages.getMixingPercentage(move, previousMove);

          extra.mixingPercentage += mixing;
          if (extra.mixingPercentage >= 100f) {
            extra.mixingPercentage = 100;
            extra.done = true;

            Droplet forward = createForwardedDroplet(move, droplet, droplet.area);
            operation.forwarding[0] = forward;

            retire(droplet);
            
          } else {
            move(droplet, move);
          }
        } else if(operation.name.equals(OperationType.dispose)) {
          Droplet droplet = operation.manipulating[0];
          
          // for now, we only dispose droplets with 1 unit size. If the droplet is larger, then split operations should occur in the assay.
          Assert.that(droplet.units.size() == 1);
          
          DropletUnit unit = droplet.units.get(0);
          Point at = unit.route.getPosition(timestamp - 1);
          
          Point waste = getClosestWasteReservoir(droplet, array);
          
          boolean arrived = (at.x == waste.x && at.y == waste.y);
          if (arrived) {
            extra.done = true;
            retire(droplet);
          } else {
            Move move = getDisposeMove(droplet, waste, runningDroplets, array);
            if (move == null) continue;
            
            Point to = at.copy().add(move.x, move.y);
            unit.route.path.add(to);
          }
          
        } else {
          // "unknown" module operations

          Assert.that(extra.module != null);  // actually it can be null when modules are more robust (when no module is assignable atm, but later is)

          Droplet droplet = operation.manipulating[0];
          Module module = extra.module;
          
          int k = 1;
          boolean dropletInside = true;
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            
            if (!GeometryUtil.inside(at.x, at.y, module.position.x, module.position.y, module.width, module.height)) {
              dropletInside = false;
              break;
            }  
          }
          
          Move move = getModuleMove(droplet, runningDroplets, module, array);
          
          // @TODO: modules may take control, if they want to.
          if (dropletInside) {
            extra.currentDurationInTimesteps += 1;
            
            if (extra.currentDurationInTimesteps >= module.duration) {
              extra.done = true;
              
              retire(droplet);
              
              Droplet forward = createForwardedDroplet(move, droplet, droplet.area);
              operation.forwarding[0] = forward;
              
              moduleAllocator.free(module);
              
            } else {
              move(droplet, move);
            }
            
          } else {
            // A detour can happen, if the droplet is within another active module.
            if (move == null) continue;
            move(droplet, move);
          }
        }
      }
       
      // select a move to droplets which did not get a move during the operation-phase.
      // This can happen, if a droplet is done with its operation or if a droplet should detour.
      for (Droplet droplet : runningDroplets) {
        Point to = droplet.units.get(0).route.getPosition(timestamp);
        if (to != null) continue;
        
        List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
        
        Point at = droplet.units.get(0).route.getPosition(timestamp - 1);
        if (validMoves.size() > 0) {
          int selectedIndex = indexSelector.selectUniformly(validMoves.size() - 1);
          
          Move move = validMoves.get(selectedIndex);
          to = at.copy().add(move.x, move.y);
          droplet.units.get(0).route.path.add(to);
        } else {
          Move move = getDetourMove(droplet, runningDroplets, array);
          to = at.copy().add(move.x, move.y);

          droplet.units.get(0).route.path.add(to);
        }
      }
      
      for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();
        OperationExtra extra = operationIdToExtra.get(operation.id);

        if (extra.done) {
          it.remove();

          Logger.log("completed %d (%s)\n", operation.id, operation.name);

          iteration = 0;

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
            }
          }
        }
      }
      
      iteration += 1;
      timestamp += 1;
      
      if (iteration == 300) {
        int k = 44;
      }
      
      // early terminate.
      if (iteration > maxIterationsPerOperation) {
        earlyTerminated = true;

        break;
      }
      
      int activeOperationsCount = runningOperations.size() + readyOperations.size();
      if (activeOperationsCount == 0) break;
    }

    retiredDroplets.addAll(runningDroplets);
    runningDroplets.clear();

    RoutingResult result = new RoutingResult();
    result.completed = !earlyTerminated;
    result.droplets.addAll(retiredDroplets);
    result.executionTime = timestamp;
    
    List<Module> modulePlacements = moduleAllocator.getModules();
    result.modules.addAll(modulePlacements);

    return result;
  }

  private Droplet createMergedDroplet(Droplet droplet0, Droplet droplet1) {
    Droplet droplet = new Droplet();
    droplet.area = droplet0.area + droplet1.area;
    droplet.id = dropletIdGenerator.getId();

    DropletUnit unit0 = droplet0.units.get(0);
    DropletUnit unit1 = droplet1.units.get(0);
    
    Point at = unit0.route.getPosition();
    
    DropletUnit copy = new DropletUnit();
    copy.route.start = timestamp;
    copy.route.path.add(at);
    
    unit0.successor = copy;
    unit1.successor = copy;
    
    droplet.units.add(copy);
    
    runningDroplets.add(droplet);
    
    return droplet;
  }


  private boolean merged(Droplet droplet0, Droplet droplet1) {
    DropletUnit u0 = droplet0.units.get(0);
    DropletUnit u1 = droplet1.units.get(0);
    
    Point at0 = u0.route.getPosition();
    Point at1 = u1.route.getPosition();
    
    return at0.x == at1.x && at0.y == at1.y;
  }

  private void move(Droplet droplet, Move move) {
    DropletUnit unit = droplet.units.get(0);
    
    Point at = unit.route.getPosition(timestamp - 1);
    Point to = at.copy().add(move.x, move.y);
    
    unit.route.path.add(to);
    
  }

  private Droplet createForwardedDroplet(Move move, Droplet d, float area) {
    DropletUnit unit = d.units.get(0);
    Point at = unit.route.getPosition();
    Point to = new Point(at).add(move.x, move.y);
    
    DropletUnit copy = new DropletUnit();
    copy.route.start = timestamp;
    copy.route.path.add(to);
    
    Droplet droplet = new Droplet();
    droplet.area = area;
    droplet.units.add(copy);
    droplet.id = dropletIdGenerator.getId();
    
    unit.successor = copy;
    
    runningDroplets.add(droplet);
    
    return droplet;
  }

  private void retire(Droplet droplet0) {
    runningDroplets.remove(droplet0);
    retiredDroplets.add(droplet0);
  }
  
  private Point getClosestWasteReservoir(Droplet droplet, BioArray array) {
    Assert.that(droplet.units.size() == 1);
    
    DropletUnit unit = droplet.units.get(0);
    Point at = unit.route.getPosition(timestamp - 1);
    
    Point selected = null;
    int minDistance = Integer.MAX_VALUE;
    
    // @cleanup: we do not allocate waste modules, should we do that?
    List<Module> disposers = moduleAllocator.getModulesOfOperationType(OperationType.dispose);
    Assert.that(disposers.size() > 0);

    for (Module disposer : disposers) {
      Point position = disposer.position;
      int distance = (int) MathUtils.getManhattanDistance(at.x, at.y, position.x, position.y);
      
      if (distance < minDistance) {
        minDistance = distance;
        selected = position;
      }
    }
    
    return selected;
  }
  
  private Move getDisposeMove(Droplet droplet, Point wasteTile, List<Droplet> droplets, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;
    
    Collections.shuffle(validMoves, RandomUtil.get());
    // if we use the manhattan distance, then reverse, turn directions yield the
                                     // same manhattan distance, meaning all moves are just as good. However, we only
                                     // select the 3 best moves, so if we don't shuffle, then the last one will
                                     // always be ignored (due to we always insert the moves in the same order).
    
    DropletUnit unit = droplet.units.get(0);
    Point at = unit.route.getPosition(timestamp - 1);

    Point target = wasteTile;
    Point next = new Point();

    validMoves.sort((move1, move2) -> {
      next.set(at).add(move1.x, move1.y);
      int distance1 = (int) MathUtils.getManhattanDistance(next.x, next.y, target.x, target.y);

      next.set(at).add(move2.x, move2.y);
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

  private Move getDetourMove(Droplet droplet, List<Droplet> droplets, BioArray array) {    
    List<Module> inUseModules = moduleAllocator.getInUseOrAlwaysLockedModules();
    // we could be within multiple modules; a dispenser within a heater or a sensor within a heater.
    List<Module> inside = new ArrayList<>();  
    for (Module other : inUseModules) {
      
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition(timestamp - 1);
      
        if (GeometryUtil.inside(at.x, at.y, other.position.x, other.position.y, other.width, other.height)) {
          inside.add(other);
        }
      }
    }
    
    Assert.that(inside.size() > 0);
    
    // @cleanup: a droplet may be within multiple droplets, movefinder does not support selecting multiple modules to ignore. so we just remove the modules the droplet is inside from the modules the movefinder checks against. @TODO: change movefinder so this is not necessary.
    inUseModules.removeAll(inside);

    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, inUseModules, array);
    
    if (validMoves.size() == 0) return null;
    Collections.shuffle(validMoves, RandomUtil.get());
    
    Point at = droplet.getCenterPosition();
    Module module = inside.get(0);
    
    float mcx = module.position.x + module.width / 2f;
    float mcy = module.position.y + module.height / 2f;
    
    Point to = new Point();

    validMoves.sort((move1, move2) -> {
      to.set(at).add(move1.x, move1.y);
      int distance1 = (int) MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);

      to.set(at).add(move2.x, move2.y);
      int distance2 = (int) MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);

      return -(distance1 - distance2);
    });

    float[] allWeights = { 50f, 33.3f, 16.6f };

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(allWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);

    return validMoves.get(bestMoveIndex);
    
    /*
    Move bestMove = null;
    float maxDistance = -1;
    
    Point to = new Point();
    
    float mcx = module.position.x + module.width / 2f;
    float mcy = module.position.y + module.height / 2f;
      
    Point at = droplet.getCenterPosition();
    Module module = inside.get(0);  // just select 1 of the modules, which droplet is within to remove away from. When the droplet is not within this module, do the same thing for the next module till the droplet is not within any module.
    
    List<Move> moves = moveFinder.getValidMoves(droplet, timestamp, droplets, inUseModules, array);
    for (Move move : moves) {
      to.set(at).add(move.x, move.y);
      
      float distance = MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);
      if (distance > maxDistance) {
        maxDistance = distance;
        bestMove = move;
      }
    }
    
    return bestMove;
     */
  }

  private Droplet createDroplet(Point position, float area) {
    DropletUnit unit = new DropletUnit();
    unit.route.start = timestamp;
    unit.route.path.add(position);
    
    Droplet droplet = new Droplet();
    droplet.area = area;
    droplet.units.add(unit);
    droplet.id = dropletIdGenerator.getId();
    
    runningDroplets.add(droplet);
    
    return droplet;
  }

  private void dispenseDroplet(Operation operation, Module dispenser) {
    reservoirManager.commit(dispenser);
    
    OperationExtra extra = operationIdToExtra.get(operation.id);
    extra.module = dispenser;
    extra.running = true;
    
    runningOperations.add(operation);

    Droplet droplet = createDroplet(dispenser.position, 1f);
    droplet.operation = operation;

    operation.manipulating[0] = droplet;
  }

  private Move getMixMove(Droplet droplet, MixingPercentages percentages, BioArray array) {
    List<Move> moves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
    Move prevMove = droplet.units.get(0).route.getMove(timestamp - 2);

    float bestPercentage = Float.NEGATIVE_INFINITY;
    Move bestMove = null;

    for (Move move : moves) {
      float percentage = percentages.getMixingPercentage(move, prevMove);

      if (percentage > bestPercentage) {
        bestPercentage = percentage;
        bestMove = move;
      }
    }

    return bestMove;
  }

  private boolean canSplit(Droplet droplet, Orientation orientation, List<Droplet> droplets, BioArray array) {
    List<Move> moves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);

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
    List<Move> validMoves = moveFinder.getValidMoves(droplet, toMerge, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;
    
    Collections.shuffle(validMoves, RandomUtil.get());
    // if we use the manhattan distance, then reverse, turn directions yield the
    // same manhattan distance, meaning all moves are just as good. However, we only
    // select the 3 best moves, so if we don't shuffle, then the last one will
    // always be ignored (due to we always insert the moves in the same order).
    
    Point at = droplet.units.get(0).route.getPosition(timestamp - 1);

    Point toMergeAt = toMerge.units.get(0).route.getPosition(timestamp - 1);
    Point toMergeTo = toMerge.units.get(0).route.getPosition(timestamp);

    Point target = toMergeTo != null ? toMergeTo : toMergeAt;
    Point next = new Point();

    validMoves.sort((move1, move2) -> {
      next.set(at).add(move1.x, move1.y);
      int distance1 = (int) MathUtils.getManhattanDistance(next.x, next.y, target.x, target.y);

      next.set(at).add(move2.x, move2.y);
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
 // move the center of the droplet to the center of the module.
    List<Move> validMoves = moveFinder.getValidMoves(droplet, module, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;

    /*
    Point at = droplet.getCenterPosition();
    Point to = new Point();
    
    int mcx = module.position.x + module.width / 2;
    int mcy = module.position.y + module.height / 2;
    
    Move bestMove = null;
    float bestMoveDistance = Float.MAX_VALUE;

    for (Move move : validMoves) {
      to.set(at).add(move.x, move.y);
      
      float distance = MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);
      if (distance < bestMoveDistance) {
        bestMoveDistance = distance;
        bestMove = move;
      }
    }

    return bestMove;
    */

    Collections.shuffle(validMoves, RandomUtil.get());
    
    Point at = droplet.getCenterPosition();
    Point to = new Point();
    
    int mcx = module.position.x + module.width / 2;
    int mcy = module.position.y + module.height / 2;
    
    validMoves.sort((move1, move2) -> {
      to.set(at).add(move1.x, move1.y);
      int distance1 = (int) MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);

      to.set(at).add(move2.x, move2.y);
      int distance2 = (int) MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);

      return distance1 - distance2;
    });

    
    float[] allWeights = { 50f, 33.3f, 16.6f };

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(allWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);

    return validMoves.get(bestMoveIndex);
  }
  
  
  private Move getSplitMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Move bestMove = null;
    int longestDistance = 0;

    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);

    Point at = droplet.units.get(0).route.getPosition(timestamp - 1);

    // select move which is furthest away from wall corner.
    Point to = new Point();
    for (Move move : validMoves) {
      to.set(at).add(move.x, move.y);

      int distance1 = (int) MathUtils.getManhattanDistance(to.x, to.y, 0, 0);
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

  // OperationAttachment/State/Temporary
  static private class OperationExtra {
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
    public Point waste;
  }
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
