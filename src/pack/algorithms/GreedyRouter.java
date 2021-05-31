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
import pack.algorithms.components.ModuleManager;
import pack.algorithms.components.MoveFinder;
import pack.algorithms.components.RandomIndexSelector;
import pack.algorithms.components.RandomUtil;
import pack.algorithms.components.ReservoirManager;
import pack.algorithms.components.SubstanceToReservoirAssigner;
import pack.algorithms.components.UidGenerator;
import pack.helpers.Assert;
import pack.helpers.GeometryUtil;
import pack.helpers.Logger;

public class GreedyRouter implements Router {

  private ConstraintsChecker checker;
  private RandomIndexSelector indexSelector;
  private MoveFinder moveFinder;
  //private ModifiedAStarPathFinder pathFinder;
  
  private ReservoirManager reservoirManager;
  private ModuleManager moduleManager;

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
    moveFinder = new MoveFinder(checker);

    SubstanceToReservoirAssigner s2rAssigner = new SubstanceToReservoirAssigner();
    List<Reservoir> reservoirs = s2rAssigner.assign(assay, array);
    reservoirManager = new ReservoirManager(reservoirs, checker);
    
    probabilitiesA = new float[] {85f, 10f, 5f};
    probabilitiesB = new float[] {50f, 33f, 17f};
    probabilitiesC = new float[] {34f, 33f, 33f};
    
    //pathFinder = new ModifiedAStarPathFinder();
    
    moduleManager = new ModuleManager(array.catalog);
    
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
          Reservoir reserved = reservoirManager.reserve(substance, runningDroplets, timestamp);
          
          if (reserved != null) {
            
            if (successor.name.equals(OperationType.merge)) {
              Operation predecessor0 = successor.inputs[0];
              Operation predecessor1 = successor.inputs[1];

              Operation sibling = (predecessor0.id == stalled.id) ? predecessor1 : predecessor0;
              if (sibling.name.equals(OperationType.dispense)) {
                String siblingSubstance = (String) sibling.attributes.get(Tags.substance);
                
                Reservoir siblingReservoir = reservoirManager.reserve(siblingSubstance, runningDroplets, timestamp);

                if (siblingReservoir == null) {
                  if (siblingSubstance.equals(substance)) {
                    int count = reservoirManager.countReservoirsContainingSubstance(siblingSubstance);

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
            String moduleIdentifier = String.format("heater%d", (int) temperature); // @TODO: hack
            stalledExtra.module = moduleManager.allocate(moduleIdentifier);
            
            // @TODO: make sure the shape fits into the module. (get width, height, otherwise) if this does not help, terminate.
            
            /*
            Droplet droplet = stalled.manipulating[0];
            
            Point target = stalledExtra.module.position;
            List<Point> path = pathFinder.search(droplet, target, stalledExtra.module, runningDroplets, array, moveFinder, moduleManager.getInUseOrAlwaysLockedModules(), timestamp - 1, 30);
            droplet.route.path.addAll(path);
            
            for (Point point : path) {
              System.out.printf(">>%s\n", point.toString());
            }
            */
          }
        }
      }

      runningOperations.sort((o1, o2) -> {
        OperationExtra e1 = operationIdToExtra.get(o1.id);
        OperationExtra e2 = operationIdToExtra.get(o2.id);
        return e1.priority - e2.priority;
      });
      
      for (Operation operation : runningOperations) {
        OperationExtra extra = operationIdToExtra.get(operation.id);
        
        if (extra.module == null) {
          // execute built-in operations
          
          if (operation.name.equals(OperationType.dispense)) {
            // Dispense location is already selected at this point. Do nothing.
            
            Droplet droplet = operation.forwarding[0];
            Point to = droplet.units.get(0).route.getPosition(timestamp);
            Assert.that(to != null);

            extra.done = true;

          } else if (operation.name.equals(OperationType.merge)) {
            Droplet droplet0 = operation.manipulating[0];
            Droplet droplet1 = operation.manipulating[1];

            Move move0 = getMergeMove(droplet0, droplet1, runningDroplets, array);
            if (move0 == null) continue;  // no move? detour!
            
            for (DropletUnit unit : droplet0.units) {
              Point at0 = unit.route.getPosition(timestamp - 1);
              Point to0 = at0.copy().add(move0.x, move0.y);
              unit.route.path.add(to0);
            }

            Move move1 = getMergeMove(droplet1, droplet0, runningDroplets, array);
            if (move1 == null) continue; // no move? detour!
            
            for (DropletUnit unit : droplet1.units) {
              Point at1 = unit.route.getPosition(timestamp - 1);
              Point to1 = at1.copy().add(move1.x, move1.y);
              unit.route.path.add(to1);
            }
            
            if (merged(droplet0, droplet1)) {
              extra.done = true;

              Droplet mergedDroplet = createMergedDroplet(droplet0, droplet1);

              operation.forwarding[0] = mergedDroplet;

              runningDroplets.add(mergedDroplet);

              // undo move, it has been set on the merged droplet
              for (DropletUnit unit : droplet0.units) {
                unit.route.path.remove(unit.route.path.size() - 1);
              }

              for (DropletUnit unit : droplet1.units) {
                unit.route.path.remove(unit.route.path.size() - 1);
              }

              runningDroplets.remove(droplet0);
              runningDroplets.remove(droplet1);

              retiredDroplets.add(droplet0);
              retiredDroplets.add(droplet1);
            }

          } else if (operation.name.equals(OperationType.split)) {
            Droplet droplet = operation.manipulating[0];

            if (canSplit(droplet, Orientation.Vertical, runningDroplets, array)) {
              extra.done = true;

              runningDroplets.remove(droplet);
              retiredDroplets.add(droplet);
              
              List<DropletUnit> units = new ArrayList<>();
              units.addAll(droplet.units);
              
              units.sort((u1, u2) -> {
                Point at1 = u1.route.getPosition(timestamp - 1);
                Point at2 = u2.route.getPosition(timestamp - 1);
                return at1.y - at2.y;
              });
              
              int units1 = droplet.units.size() / 2;
              
              float area1 = units1;
              float area2 = droplet.area - area1;

              // select the bottom droplet-units to go down
              List<DropletUnit> downUnits = units.subList(0, units1);
              Droplet d1 = createForwardedDroplet(Move.Down, downUnits, area1);
              
              // select the top droplet-units to go up
              List<DropletUnit> upUnits = units.subList(units1, droplet.units.size());
              Droplet d2 = createForwardedDroplet(Move.Up, upUnits, area2);
              
              runningDroplets.add(d1);
              runningDroplets.add(d2);
              
              operation.forwarding[0] = d1;
              operation.forwarding[1] = d2;
              
            } else if (canSplit(droplet, Orientation.Horizontal, runningDroplets, array)) {
              extra.done = true;

              runningDroplets.remove(droplet);
              retiredDroplets.add(droplet);
              
              List<DropletUnit> units = new ArrayList<>();
              units.addAll(droplet.units);
              
              units.sort((u1, u2) -> {
                Point at1 = u1.route.getPosition(timestamp - 1);
                Point at2 = u2.route.getPosition(timestamp - 1);
                return at1.x - at2.x;
              });
              
              int units1 = droplet.units.size() / 2;
              
              float area1 = units1;
              float area2 = droplet.area - area1;

              // select the left droplet-units to go left
              List<DropletUnit> leftUnits = units.subList(0, units1);
              Droplet d1 = createForwardedDroplet(Move.Left, leftUnits, area1);
              
              // select the right droplet-units to go right
              List<DropletUnit> rightUnits = units.subList(units1, droplet.units.size());
              Droplet d2 = createForwardedDroplet(Move.Right, rightUnits, area2);
              
              runningDroplets.add(d1);
              runningDroplets.add(d2);
              
              operation.forwarding[0] = d1;
              operation.forwarding[1] = d2;
              
            } else {
              // move somewhere, where it can split.
              Move move = getSplitMove(droplet, runningDroplets, array);
              if (move == null) continue;
              
              for (DropletUnit unit : droplet.units) {
                Point at = unit.route.getPosition(timestamp - 1);
                Point to = at.copy().add(move.x, move.y);
                unit.route.path.add(to);
              }
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

              Droplet forward = createForwardedDroplet(move, droplet.units, droplet.area);
              operation.forwarding[0] = forward;

              runningDroplets.remove(droplet);
              retiredDroplets.add(droplet);

              runningDroplets.add(forward);
              
            } else {
              for (DropletUnit unit : droplet.units) {
                Point at = unit.route.getPosition(timestamp - 1);
                Point to = at.copy().add(move.x, move.y);
                unit.route.path.add(to);
              }
            }
          } else if(operation.name.equals(OperationType.dispose)) { 
            /*
            Droplet droplet = operation.manipulating[0];
            
            Point at = droplet.route.getPosition(timestamp - 1);
            
            Point waste = null; // @TODO
            
            boolean arrived = (at.x == extra.waste.x && at.y == extra.waste.y);
            if (arrived) {
              extra.done = true;
              runningDroplets.remove(droplet);
            } else {
              Move move = getDisposeMove(droplet, extra.waste, runningDroplets, array);
              if (move == null) continue;
              
              Point to = at.copy().add(move.x, move.y);
              droplet.route.path.add(to);
            }
            
            */
            throw new IllegalStateException("unsupported operation!");
            
          } else {
            throw new IllegalStateException("unsupported operation!");
          }
          
          
        } else {
          // module operations

          Droplet droplet = operation.manipulating[0];
          
          Module module = extra.module;
          
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
              
              runningDroplets.remove(droplet);
              retiredDroplets.add(droplet);
              
              Droplet forward = createModuleDroplet(move, droplet.units, droplet.area);
              operation.forwarding[0] = forward;
              runningDroplets.add(forward);
              
              moduleManager.free(module);
              
            } else {
              for (DropletUnit unit : droplet.units) {
                Point at = unit.route.getPosition(timestamp - 1);
                Point to = at.copy().add(move.x, move.y);
                unit.route.path.add(to);
              }
            }
            
          } else {
            // A detour can happen, if the droplet is within another active module.
            if (move == null) continue;
            
            for (DropletUnit unit : droplet.units) {
              Point at = unit.route.getPosition(timestamp - 1);
              Point to = at.copy().add(move.x, move.y);
              unit.route.path.add(to);
            }
          }
        }
      }
       
      // select a move to droplets which did not get a move during the operation-phase.
      // This can happen, if a droplet is done with its operation or if a droplet should detour.
      for (Droplet droplet : runningDroplets) {
        if (droplet.hasPosition(timestamp)) continue;
        
        List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
        
        if (validMoves.size() > 0) {
          int selectedIndex = indexSelector.selectUniformly(validMoves.size() - 1);
          Move move = validMoves.get(selectedIndex);
          
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);
            unit.route.path.add(to);
          }
          
        } else {
          Move move = getDetourMove(droplet, runningDroplets, array);
          
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);
            unit.route.path.add(to);
          }
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
    result.reservoirs.addAll(reservoirManager.getReservoirs());
    result.executionTime = timestamp;
    
    List<Module> modulePlacements = moduleManager.getModules();
    result.modules.addAll(modulePlacements);

    return result;
  }

  private boolean merged(Droplet droplet0, Droplet droplet1) {
    // @speed: use a grid instead, fill up the grid with positions, and check if a droplet-units neighbour contains 1 or more of the other droplets unit -> O(n) instead of O(n^2)
    
    for (DropletUnit unit0 : droplet0.units) {
      Point at0 = unit0.route.getPosition(timestamp);
      
      for (DropletUnit unit1 : droplet1.units) {
        Point at1 = unit1.route.getPosition(timestamp);
        
        int distance = (int) MathUtils.getManhattanDistance(at0.x, at0.y, at1.x, at1.y);
        Assert.that(distance != 0);
        if (distance == 1) return true;
        
      }
    }
    
    return false;
  }
  
  private Move getDisposeMove(Droplet droplet, Point wasteTile, List<Droplet> droplets, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;
    
    Collections.shuffle(validMoves, RandomUtil.get());
    // if we use the manhattan distance, then reverse, turn directions yield the
                                     // same manhattan distance, meaning all moves are just as good. However, we only
                                     // select the 3 best moves, so if we don't shuffle, then the last one will
                                     // always be ignored (due to we always insert the moves in the same order).
    
    Point at = droplet.units.get(0).route.getPosition(timestamp - 1);

    Point target = wasteTile;
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

  private Move getDetourMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Point at = droplet.units.get(0).route.getPosition(timestamp - 1);
    
    List<Module> inUseModules = moduleManager.getInUseOrAlwaysLockedModules();
    
    Module module = null;
    for (Module other : inUseModules) {
      if (GeometryUtil.inside(at.x, at.y, other.position.x, other.position.y, other.width, other.height)) {
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

  private Droplet createDroplet(Point position, float area) {
    Droplet droplet = new Droplet();
    droplet.area = area;
    droplet.id = dropletIdGenerator.getId();
    
    DropletUnit unit = new DropletUnit();
    unit.route.start = timestamp;
    unit.route.path.add(position);
    
    droplet.units.add(unit);
    
    return droplet;
  }
    
  
  private Droplet createForwardedDroplet(Move move, List<DropletUnit> units, float area) {
    Droplet droplet = new Droplet();
    droplet.area = area;
    droplet.id = dropletIdGenerator.getId();
    
    for (DropletUnit unit : units) {
      Point at = new Point(unit.route.getPosition()).add(move.x, move.y);
      
      DropletUnit copy = new DropletUnit();
      copy.route.start = timestamp;
      copy.route.path.add(at);
      
      droplet.units.add(copy);
    }
    
    return droplet;
  }
  
  private Droplet createMergedDroplet(Droplet droplet0, Droplet droplet1) {
    Droplet droplet = new Droplet();
    droplet.area = droplet0.area + droplet1.area;
    droplet.id = dropletIdGenerator.getId();
    
    for (DropletUnit unit : droplet0.units) {
      Point at = unit.route.getPosition();
      
      DropletUnit copy = new DropletUnit();
      copy.route.start = timestamp;
      copy.route.path.add(at);
      
      droplet.units.add(copy);
    }
    
    for (DropletUnit unit : droplet1.units) {
      Point at = unit.route.getPosition();
      
      DropletUnit copy = new DropletUnit();
      copy.route.start = timestamp;
      copy.route.path.add(at);
      
      droplet.units.add(copy);
    }
    
    return droplet;
  }

  private Droplet createModuleDroplet(Move move, List<DropletUnit> units, float area) {
    Droplet droplet = new Droplet();
    droplet.area = area;
    droplet.id = dropletIdGenerator.getId();
    
    for (DropletUnit unit : units) {
      Point at = new Point(unit.route.getPosition()).add(move.x, move.y);
      
      DropletUnit copy = new DropletUnit();
      copy.route.start = timestamp;
      copy.route.path.add(at);
      
      droplet.units.add(copy);
    }
    
    return droplet;
  }

  private void dispenseDroplet(Operation operation, Point position) {
    OperationExtra extra = operationIdToExtra.get(operation.id);
    extra.running = true;
    
    runningOperations.add(operation);

    Droplet droplet = createDroplet(position, 1f);
    droplet.operation = operation;

    runningDroplets.add(droplet);

    operation.forwarding[0] = droplet;
  }

  private Move getMixMove(Droplet droplet, MixingPercentages percentages, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
    Move prevMove = droplet.units.get(0).route.getMove(timestamp - 2);

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
    
    DropletUnit sourceUnit = droplet.units.get(0);
    DropletUnit targetUnit = toMerge.units.get(0);
    
    Point at = sourceUnit.route.getPosition(timestamp - 1);

    Point toMergeAt = targetUnit.route.getPosition(timestamp - 1);
    Point toMergeTo = targetUnit.route.getPosition(timestamp);

    Point target = (toMergeTo != null) ? toMergeTo : toMergeAt;
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
    // move the center of the droplet to the center of the module.
    
    Move bestMove = null;
    float bestMoveDistance = Float.MAX_VALUE;
    
    List<Move> validMoves = moveFinder.getValidMoves(droplet, module, timestamp, droplets, moduleManager.getInUseOrAlwaysLockedModules(), array);
    
    Point at = droplet.getCenterPosition();
    
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

    Point at = droplet.getCenterPosition();

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
