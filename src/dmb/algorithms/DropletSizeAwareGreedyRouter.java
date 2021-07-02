package dmb.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dmb.components.BoundingBox;
import dmb.components.ConstraintsChecker;
import dmb.components.RandomIndexSelector;
import dmb.components.ReservoirManager;
import dmb.components.SubstanceToReservoirAssigner;
import dmb.components.input.AttributeTag;
import dmb.components.input.AttributeTags;
import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.mixingpercentages.MixingPercentages;
import dmb.components.module.MinModuleAllocationStrategy;
import dmb.components.module.Module;
import dmb.components.module.ModuleAllocationStrategy;
import dmb.components.module.ModuleAllocator;
import dmb.components.moves.Move;
import dmb.components.moves.MoveFinder;
import dmb.components.moves.MultiCellMoveFinder;
import dmb.components.prioritizer.Prioritizer;
import dmb.components.prioritizer.WeightedChainPrioritizer;
import dmb.components.shaping.DropletReshaper;
import dmb.components.shaping.DropletReshapingResult;
import dmb.components.shaping.DropletShape;
import dmb.components.shaping.DropletShapeSelector;
import dmb.helpers.Assert;
import dmb.helpers.GeometryUtil;
import dmb.helpers.Logger;
import dmb.helpers.RandomUtil;
import dmb.helpers.UidGenerator;
import framework.input.Droplet;
import framework.input.DropletUnit;
import framework.math.MathUtils;

public class DropletSizeAwareGreedyRouter implements Router {

  private boolean reshape = true;

  private ConstraintsChecker checker;
  private RandomIndexSelector indexSelector;
  private MoveFinder moveFinder;
  private DropletReshaper reshaper;
  private DropletShapeSelector shapeSelector;

  private Prioritizer prioritizer;

  private ReservoirManager reservoirManager;
  private ModuleAllocator moduleAllocator;

  private List<Operation> readyOperations;
  private List<Operation> runningOperations;
  private List<Operation> reshapeOperations;
  private List<Operation> completedOperations;

  private List<Droplet> runningDroplets; // all the droplets currently on the array

  private List<Droplet> retiredDroplets;

  private Map<Integer, OperationExtra> operationIdToExtra;
  private Map<Integer, DropletExtra> dropletIdToExtra;

  private UidGenerator dropletIdGenerator;

  private int maxIterationsPerOperation;
  private int iteration;

  private int timestamp;

  private float[] moveWeights;

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
    moveFinder = new MultiCellMoveFinder(checker);
    reshaper = new DropletReshaper(moveFinder, array);
    shapeSelector = new DropletShapeSelector();

    ModuleAllocationStrategy strategy = new MinModuleAllocationStrategy();
    moduleAllocator = new ModuleAllocator(array.catalog, strategy);

    SubstanceToReservoirAssigner s2rAssigner = new SubstanceToReservoirAssigner();
    s2rAssigner.assign(assay, array, moduleAllocator);
    reservoirManager = new ReservoirManager(moduleAllocator, checker);

    prioritizer = new WeightedChainPrioritizer();

    maxIterationsPerOperation = 500; // if no operation terminates before iteration is this value, then we assume that no solution can be found.
    iteration = 0;

    operationIdToExtra = new HashMap<>();
    dropletIdToExtra = new HashMap<>();

    moveWeights = new float[] { 50f, 33.3f, 16.6f };

    List<Operation> operations = assay.getOperations();
    for (Operation operation : operations) {
      OperationExtra extra = new OperationExtra();
      extra.doneWithOperation = false;
      extra.completelyDone = false;
      extra.active = false;

      operationIdToExtra.put(operation.id, extra);
    }

    dropletIdGenerator = new UidGenerator();

    readyOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();
    reshapeOperations = new ArrayList<>();
    completedOperations = new ArrayList<>();

    runningDroplets = new ArrayList<>();
    retiredDroplets = new ArrayList<>();

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

          String substance = (String) stalled.attributes.get(AttributeTags.substance);
          Module reserved = reservoirManager.reserve(substance, runningDroplets, timestamp);

          if (reserved != null) {

            if (successor != null && successor.name.equals(OperationType.merge)) {
              Operation predecessor0 = successor.inputs[0];
              Operation predecessor1 = successor.inputs[1];

              Operation sibling = (predecessor0.id == stalled.id) ? predecessor1 : predecessor0;
              if (sibling.name.equals(OperationType.dispense)) {
                String siblingSubstance = (String) sibling.attributes.get(AttributeTags.substance);

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
                if (siblingExtra.completelyDone) {
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

          if (stalled.name.equals(OperationType.heating)) {
            float temperature = (float) stalled.attributes.get(AttributeTags.temperature);

            Droplet droplet = stalled.manipulating[0];
            BoundingBox boundingBox = droplet.getBoundingBox();
            Module module = moduleAllocator.allocate(OperationType.heating, boundingBox.width, boundingBox.height, new AttributeTag(AttributeTags.temperature, temperature));
            stalledExtra.module = module;

          } else if (stalled.name.equals(OperationType.detection)) {
            String sensor = (String) stalled.attributes.get(AttributeTags.sensor);

            Droplet droplet = stalled.manipulating[0];
            BoundingBox boundingBox = droplet.getBoundingBox();
            Module module = moduleAllocator.allocate(OperationType.detection, boundingBox.width, boundingBox.height, new AttributeTag(AttributeTags.sensor, sensor));
            stalledExtra.module = module;
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
            Droplet forwarded = createForwardedDroplet(Move.None, droplet.units, droplet.area);

            moduleAllocator.free(dispenser);

            operation.forwarding[0] = forwarded;
            extra.doneWithOperation = true;
          } else {
            move(droplet, Move.None);
          }

        } else if (operation.name.equals(OperationType.merge)) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];

          Move move0 = getMergeMove(droplet0, droplet1, runningDroplets, array);
          if (move0 == null) continue; // no move? detour!

          move(droplet0, move0);

          Move move1 = getMergeMove(droplet1, droplet0, runningDroplets, array);
          if (move1 == null) continue; // no move? detour!

          move(droplet1, move1);

          if (merged(droplet0, droplet1)) {
            extra.doneWithOperation = true;

            Droplet mergedDroplet = createMergedDroplet(droplet0, droplet1);
            operation.forwarding[0] = mergedDroplet;

            // undo move, it has been set on the merged droplet
            for (DropletUnit unit : droplet0.units) {
              unit.route.path.remove(unit.route.path.size() - 1);
            }

            for (DropletUnit unit : droplet1.units) {
              unit.route.path.remove(unit.route.path.size() - 1);
            }

            retire(droplet0);
            retire(droplet1);
          }

        } else if (operation.name.equals(OperationType.split)) {
          Droplet droplet = operation.manipulating[0];

          if (canSplit(droplet, Orientation.Vertical, runningDroplets, array)) {
            extra.doneWithOperation = true;

            retire(droplet);

            List<DropletUnit> units = new ArrayList<>(droplet.units);
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

            operation.forwarding[0] = d1;
            operation.forwarding[1] = d2;

          } else if (canSplit(droplet, Orientation.Horizontal, runningDroplets, array)) {
            extra.doneWithOperation = true;

            retire(droplet);

            List<DropletUnit> units = new ArrayList<>(droplet.units);
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

          DropletUnit unit = droplet.units.get(0);
          Move previousMove = unit.route.getMove(timestamp - 2);
          float mixing = percentages.getMixingPercentage(move, previousMove);

          extra.mixingPercentage += mixing;
          if (extra.mixingPercentage >= 100f) {
            extra.mixingPercentage = 100;
            extra.doneWithOperation = true;

            Droplet forward = createForwardedDroplet(move, droplet.units, droplet.area);
            operation.forwarding[0] = forward;

            retire(droplet);

          } else {
            move(droplet, move);
          }
        } else if (operation.name.equals(OperationType.dispose)) {
          Droplet droplet = operation.manipulating[0];

          // for now, we only dispose droplets with 1 unit size. If the droplet is larger, then split operations should occur in the assay.
          //Assert.that(droplet.units.size() == 1);

          DropletUnit unit = droplet.units.get(0);
          Point at = unit.route.getPosition(timestamp - 1);

          Point waste = getClosestWasteReservoir(droplet, array);

          boolean arrived = (at.x == waste.x && at.y == waste.y);
          if (arrived) {
            extra.doneWithOperation = true;
            retire(droplet);
          } else {
            Move move = getDisposeMove(droplet, waste, runningDroplets, array);
            if (move == null) continue;
            move(droplet, move);
          }

        } else {
          // "unknown" module operations

          Assert.that(extra.module != null); // actually it can be null when modules are more robust (when no module is assignable atm, but later is)

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

          if (dropletInside) {
            extra.currentDurationInTimesteps += 1;

            if (extra.currentDurationInTimesteps >= module.duration) {
              extra.doneWithOperation = true;

              retire(droplet);

              Droplet forward = createForwardedDroplet(move, droplet.units, droplet.area);
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

      for (Iterator<Operation> it = reshapeOperations.iterator(); it.hasNext();) {
        Operation operation = it.next();

        Assert.that(operation.name.equals(OperationType.merge) || operation.name.equals(OperationType.split));

        boolean allDone = true;
        for (Droplet droplet : operation.forwarding) {
          DropletExtra dropletExtra = getDropletExtra(droplet.id);

          if (!dropletExtra.reshaping) continue;

          DropletReshapingResult result = reshaper.step(droplet, runningDroplets, moduleAllocator.getInUseOrAlwaysLockedModules(), timestamp);
          if (result.done) {
            dropletExtra.reshaping = false;
          } else {
            allDone = false;

            if (!result.progress) {
              // undo move, and detour if no progress has been made.
              for (DropletUnit unit : droplet.units) {
                unit.route.path.remove(unit.route.path.size() - 1);
              }
            }
          }
        }

        if (allDone) {
          OperationExtra extra = operationIdToExtra.get(operation.id);
          extra.completelyDone = true;

          it.remove();
          completedOperations.add(operation);
        }
      }

      // select a move to droplets which did not get a move during the operation-phase.
      // This can happen, if a droplet is done with its operation or if a droplet should detour.
      for (Droplet droplet : runningDroplets) {
        if (droplet.hasPosition(timestamp)) continue;

        List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
        if (validMoves.size() > 0) {
          int selectedIndex = indexSelector.selectUniformly(validMoves.size() - 1);
          Move move = validMoves.get(selectedIndex);
          move(droplet, move);
        } else {
          Move move = getDetourMove(droplet, runningDroplets, array);
          move(droplet, move);
        }
      }

      if (reshape) {

        for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
          Operation operation = it.next();
          OperationExtra extra = operationIdToExtra.get(operation.id);

          if (extra.doneWithOperation) {
            it.remove();

            if (operation.name.equals(OperationType.merge) || operation.name.equals(OperationType.split)) {
              reshapeOperations.add(operation);

              for (Droplet droplet : operation.forwarding) {
                DropletExtra dropletExtra = getDropletExtra(droplet.id);

                dropletExtra.reshaping = true;
                DropletShape shape = shapeSelector.select(droplet);
                reshaper.reshape(droplet, shape);
              }

            } else {
              extra.completelyDone = true;
              completedOperations.add(operation);
            }
          }
        }

      } else {

        for (Iterator<Operation> it = runningOperations.iterator(); it.hasNext();) {
          Operation operation = it.next();
          OperationExtra extra = operationIdToExtra.get(operation.id);

          if (extra.doneWithOperation) {
            it.remove();

            extra.completelyDone = true;
            completedOperations.add(operation);
          }
        }
      }

      for (Operation operation : completedOperations) {
        Logger.log("completed %d (%s)\n", operation.id, operation.name);

        iteration = 0;

        for (Operation descendant : operation.outputs) {
          if (descendant == null) continue;

          OperationExtra descendantExtra = operationIdToExtra.get(descendant.id);
          if (descendantExtra.active) continue;

          boolean canRun = true;
          for (Operation input : descendant.inputs) {
            OperationExtra inputExtra = operationIdToExtra.get(input.id);
            if (!inputExtra.completelyDone) canRun = false;
          }

          if (canRun) {
            descendantExtra.active = true;
            readyOperations.add(descendant);
          }
        }
      }

      completedOperations.clear();

      iteration += 1;
      timestamp += 1;

      // early terminate.
      if (iteration > maxIterationsPerOperation) {
        earlyTerminated = true;
        break;
      }

      int activeOperationsCount = countRunningDroplets();
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

  private DropletExtra getDropletExtra(int dropletId) {
    DropletExtra extra = dropletIdToExtra.get(dropletId);

    if (extra == null) {
      extra = new DropletExtra();
      dropletIdToExtra.put(dropletId, extra);
    }

    return extra;
  }

  private int countRunningDroplets() {
    return runningOperations.size() + readyOperations.size() + reshapeOperations.size();
  }

  private void move(Droplet droplet, Move move) {
    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition(timestamp - 1);
      Point to = at.copy().add(move.x, move.y);
      unit.route.path.add(to);
    }
  }

  private void retire(Droplet droplet) {
    Assert.that(runningDroplets.contains(droplet));

    runningDroplets.remove(droplet);
    retiredDroplets.add(droplet);
  }

  private Point getClosestWasteReservoir(Droplet droplet, BioArray array) {
    //Assert.that(droplet.units.size() == 1);

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

  private boolean merged(Droplet droplet0, Droplet droplet1) {
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
    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;

    // if we use the manhattan distance, then reverse, turn directions yield the
    // same manhattan distance, meaning all moves are just as good. However, we only
    // select the 3 best moves, so if we don't shuffle, then the last one will
    // always be ignored (due to we always insert the moves in the same order).
    Collections.shuffle(validMoves, RandomUtil.get());

    DropletUnit unit = droplet.units.get(0);
    Point at = unit.route.getPosition(timestamp - 1);

    Point target = wasteTile;
    Point to = new Point();

    validMoves.sort((move1, move2) -> {
      to.set(at).add(move1.x, move1.y);
      int distance1 = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);

      to.set(at).add(move2.x, move2.y);
      int distance2 = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);

      return distance1 - distance2;
    });

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(moveWeights, 0, weights, 0, candidateSize);

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

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(moveWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);

    return validMoves.get(bestMoveIndex);

    /*
    Move bestMove = null;
    float maxDistance = -1;
    
    Point to = new Point();
    
    float mcx = module.position.x + module.width / 2f;
    float mcy = module.position.y + module.height / 2f;
      
    // @TODO: probabilistic moves, it possible for deadlocks @create test which does deadlock!
    
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

      unit.successor = copy;

      droplet.units.add(copy);
    }

    runningDroplets.add(droplet);

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

      unit.successor = copy;

      droplet.units.add(copy);
    }

    for (DropletUnit unit : droplet1.units) {
      Point at = unit.route.getPosition();

      DropletUnit copy = new DropletUnit();
      copy.route.start = timestamp;
      copy.route.path.add(at);

      unit.successor = copy;

      droplet.units.add(copy);
    }

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

    runningDroplets.add(droplet);

    operation.manipulating[0] = droplet;
  }

  private Move getMixMove(Droplet droplet, MixingPercentages percentages, BioArray array) {
    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, runningDroplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);

    DropletUnit unit = droplet.units.get(0);
    Move prevMove = unit.route.getMove(timestamp - 2);

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

    // if we use the manhattan distance, then reverse, turn directions yield the
    // same manhattan distance, meaning all moves are just as good. However, we only
    // select the 3 best moves, so if we don't shuffle, then the last one will
    // always be ignored (due to we always insert the moves in the same order).
    Collections.shuffle(validMoves, RandomUtil.get());

    DropletUnit sourceUnit = droplet.units.get(0);
    DropletUnit targetUnit = toMerge.units.get(0);

    Point at = sourceUnit.route.getPosition(timestamp - 1);

    Point toMergeAt = targetUnit.route.getPosition(timestamp - 1);
    Point toMergeTo = targetUnit.route.getPosition(timestamp);

    Point target = (toMergeTo != null) ? toMergeTo : toMergeAt;
    Point to = new Point();

    validMoves.sort((move1, move2) -> {
      to.set(at).add(move1.x, move1.y);
      int distance1 = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);

      to.set(at).add(move2.x, move2.y);
      int distance2 = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);

      return distance1 - distance2;
    });

    int candidateSize = (int) MathUtils.clamp(1, 3, validMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(moveWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);

    return validMoves.get(bestMoveIndex);

  }

  private Move getModuleMove(Droplet droplet, List<Droplet> droplets, Module module, BioArray array) {
    // move the center of the droplet to the center of the module.
    List<Move> validMoves = moveFinder.getValidMoves(droplet, module, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);
    if (validMoves.size() == 0) return null;

    Collections.shuffle(validMoves, RandomUtil.get());

    /*
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

    Point to = new Point();

    {
      Point at = droplet.getCenterPosition();

      int mcx = module.position.x + module.width / 2;
      int mcy = module.position.y + module.height / 2;

      validMoves.sort((move1, move2) -> {
        to.set(at).add(move1.x, move1.y);
        int distance1 = (int) MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);

        to.set(at).add(move2.x, move2.y);
        int distance2 = (int) MathUtils.getManhattanDistance(to.x, to.y, mcx, mcy);

        return distance1 - distance2;
      });
    }

    List<Move> selectedMoves = new ArrayList<>();
    for (Move move : validMoves) {

      boolean dropletInside = true;
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition(timestamp - 1);
        to.set(at).add(move.x, move.y);

        if (!GeometryUtil.inside(to.x, to.y, module.position.x, module.position.y, module.width, module.height)) {
          dropletInside = false;
          break;
        }
      }

      if (dropletInside) selectedMoves.add(move);
    }

    if (selectedMoves.size() == 0) {
      selectedMoves.addAll(validMoves);
    }

    int candidateSize = (int) MathUtils.clamp(1, 3, selectedMoves.size());

    float[] weights = new float[candidateSize];
    System.arraycopy(moveWeights, 0, weights, 0, candidateSize);

    int bestMoveIndex = indexSelector.select(weights);

    return selectedMoves.get(bestMoveIndex);
  }

  private Move getSplitMove(Droplet droplet, List<Droplet> droplets, BioArray array) {
    Move bestMove = null;
    int longestDistance = 0;

    List<Move> validMoves = moveFinder.getValidMoves(droplet, timestamp, droplets, moduleAllocator.getInUseOrAlwaysLockedModules(), array);

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

  private class DropletExtra {
    public boolean reshaping;
  }

  private class OperationExtra {
    public boolean active;
    public boolean running;

    public boolean doneWithOperation;
    public boolean completelyDone;

    public int forwardIndex;

    public float mixingPercentage; // only used for mixing operations.

    public int currentDurationInTimesteps;
    public Module module;
  }
}
