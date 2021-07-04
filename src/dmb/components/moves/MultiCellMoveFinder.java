package dmb.components.moves;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Point;
import dmb.components.ConstraintsChecker;
import dmb.components.input.BioArray;
import dmb.components.module.Module;
import dmb.helpers.GeometryUtil;
import framework.input.Droplet;
import framework.input.DropletUnit;
import framework.math.MathUtils;

/**
 * MultiCellMoveFinder finds the valid moves for a droplet which can occupy
 * multiple cells. MultiCellMoveFinder assumes two droplets merge if they are
 * vertical or horizontal adjacent.
 * 
 */
public class MultiCellMoveFinder extends MoveFinder {

  private ConstraintsChecker checker;

  public MultiCellMoveFinder(ConstraintsChecker checker) {
    this.checker = checker;
  }

  @Override
  public List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    List<Move> validMoves = new ArrayList<>();

    for (Move move : Move.values()) {
      if (!isWithinArray(move, dropletUnit, timestamp, array)) continue;
      if (isWithinModule(move, dropletUnit, targetModule, modules, timestamp)) continue;
      if (!satifiesDropletDropletConstraints(move, dropletUnit, droplet, droplets, timestamp)) continue;
      if (!satisfiesInterDropletUnitConstraints(move, dropletUnit, droplet, timestamp)) continue;

      validMoves.add(move);
    }

    return validMoves;
  }

  private boolean satisfiesInterDropletUnitConstraints(Move move, DropletUnit dropletUnit, Droplet droplet, int timestamp) {
    Point at = dropletUnit.route.getPosition(timestamp - 1);
    Point to = at.copy().add(move.x, move.y);

    // droplet units within the same droplet.
    for (DropletUnit brotherUnit : droplet.units) {
      if (brotherUnit == dropletUnit) continue;

      Point brotherTo = brotherUnit.route.getPosition(timestamp);
      if (brotherTo != null) {
        if (to.x == brotherTo.x && to.y == brotherTo.y) return false;
      } else {
        Point brotherAt = brotherUnit.route.getPosition(timestamp - 1);
        if (to.x == brotherAt.x && to.y == brotherAt.y) return false;
      }
    }

    return true;
  }

  @Override
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    List<Move> validMoves = new ArrayList<>();

    for (Move move : Move.values()) {
      if (!isWithinArray(move, droplet, timestamp, array)) continue;
      if (isWithinModule(move, droplet, targetModule, modules, timestamp)) continue;

      // Special case for droplets which should merge with another droplet.
      if (mergeSibling != null) {
        boolean satifiesConstraints = satifiesSpacingConstraints(droplet, mergeSibling, timestamp, move);

        boolean siblingAlreadyMoved = mergeSibling.hasPosition(timestamp);
        if (siblingAlreadyMoved) {
          boolean willMerge = willMerge(droplet, mergeSibling, timestamp, move);
          if (!satifiesConstraints && !willMerge) continue;
        } else {
          if (!satifiesConstraints) continue;
        }
      }

      // skip moves which do not satisfy droplet-droplet constraints.
      if (!satifiesDropletDropletConstraints(move, droplet, mergeSibling, droplets, timestamp)) continue;

      validMoves.add(move);
    }

    return validMoves;
  }

  private boolean satifiesDropletDropletConstraints(Move move, Droplet droplet, Droplet mergeSibling, List<Droplet> droplets, int timestamp) {
    for (Droplet other : droplets) {
      if (other.id == droplet.id) continue;
      if (mergeSibling != null && other.id == mergeSibling.id) continue;

      if (!satifiesSpacingConstraints(droplet, other, timestamp, move)) return false;
    }

    return true;
  }

  private boolean satifiesDropletDropletConstraints(Move move, DropletUnit dropletUnit, Droplet droplet,
      List<Droplet> droplets, int timestamp) {

    for (Droplet other : droplets) {
      if (other.id == droplet.id) continue;
      if (!satifiesSpacingConstraints(dropletUnit, other, timestamp, move)) return false;
    }

    return true;
  }

  private boolean isWithinModule(Move move, Droplet droplet, Module targetModule, List<Module> modules, int timestamp) {
    // skip moves which overlap modules, unless the module is the target module.
    for (DropletUnit unit : droplet.units) {
      if (isWithinModule(move, unit, targetModule, modules, timestamp)) return true;
    }

    return false;
  }

  private boolean isWithinModule(Move move, DropletUnit unit, Module targetModule, List<Module> modules, int timestamp) {
    Point at = unit.route.getPosition(timestamp - 1);
    Point to = at.copy().add(move.x, move.y);

    for (Module other : modules) {
      if (other == targetModule) continue;
      if (GeometryUtil.inside(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) return true;
    }

    return false;
  }

  private boolean isWithinArray(Move move, Droplet droplet, int timestamp, BioArray array) {
    for (DropletUnit unit : droplet.units) {
      if (!isWithinArray(move, unit, timestamp, array)) return false;
    }

    return true;
  }

  private boolean isWithinArray(Move move, DropletUnit unit, int timestamp, BioArray array) {
    Point at = unit.route.getPosition(timestamp - 1);
    Point to = at.copy().add(move.x, move.y);
    return GeometryUtil.inside(to.x, to.y, array.width, array.height);
  }

  private boolean willMerge(Droplet droplet, Droplet mergeSibling, int timestamp, Move move) {
    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition(timestamp - 1);
      Point to = at.copy().add(move.x, move.y);

      for (DropletUnit siblingUnit : mergeSibling.units) {
        Point siblingTo = siblingUnit.route.getPosition(timestamp);
        int distance = (int) MathUtils.getManhattanDistance(to.x, to.y, siblingTo.x, siblingTo.y);
        if (distance == 1) return true;
      }
    }

    return false;
  }

  private boolean satifiesSpacingConstraints(Droplet droplet, Droplet other, int timestamp, Move move) {
    for (DropletUnit unit : droplet.units) {
      if (!satifiesSpacingConstraints(unit, other, timestamp, move)) return false;
    }

    return true;
  }

  private boolean satifiesSpacingConstraints(DropletUnit unit, Droplet other, int timestamp, Move move) {
    Point at = unit.route.getPosition(timestamp - 1);
    Point to = at.copy().add(move.x, move.y);

    for (DropletUnit otherUnits : other.units) {
      Point otherAt = otherUnits.route.getPosition(timestamp - 1);
      Point otherTo = otherUnits.route.getPosition(timestamp);

      if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) return false;
    }

    return true;
  }
}
