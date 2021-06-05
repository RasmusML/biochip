package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

import engine.math.MathUtils;
import pack.algorithms.components.ConstraintsChecker;
import pack.helpers.GeometryUtil;

public class MultiCellMoveFinder extends MoveFinder {
  
  private ConstraintsChecker checker;
  
  // The behavior of single-unit droplets and multi-units droplets are different. In single-unit droplet, we assume after a merge, the two droplets occupy the same cell and not adjacent tiles. Multi-unit droplets will occupy adjacent cells.
  
  public MultiCellMoveFinder(ConstraintsChecker checker) {
    this.checker = checker;
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
      
      // skip moves which does not satisfy droplet-droplet constraints.
      if (!satifiesDropletDropletConstraints(move, droplet, mergeSibling, droplets, timestamp)) continue;

      // a move is only added, if the move is valid for all droplet units.
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
  
  private boolean isWithinModule(Move move, Droplet droplet, Module targetModule, List<Module> modules, int timestamp) {
    // skip moves which overlap modules, unless the module is the target module.
    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition(timestamp - 1);
      Point to = at.copy().add(move.x, move.y);
      
      for (Module other : modules) {
        if (other == targetModule) continue;
        if (GeometryUtil.inside(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) return true;
      }
    }
    
    return false;
  }
  
  private boolean isWithinArray(Move move, Droplet droplet, int timestamp, BioArray array) {
    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition(timestamp - 1);
      Point to = at.copy().add(move.x, move.y);

      if (!GeometryUtil.inside(to.x, to.y, array.width, array.height)) return false;
    }
    
    return true;
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
      Point at = unit.route.getPosition(timestamp - 1);
      Point to = at.copy().add(move.x, move.y);

      for (DropletUnit otherUnits : other.units) {
        Point otherAt = otherUnits.route.getPosition(timestamp - 1);
        Point otherTo = otherUnits.route.getPosition(timestamp);
        
        if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) return false;
      }
    }

    return true;
  }
}


/*
//Don't let the droplet-unit of a droplet overlap. If another unit has already moved, then we check against that position.
// Otherwise, we check against the current position.
for (DropletUnit unit : droplet.units) {
  if (dropletUnit == unit) continue;
  
  Point otherAt = unit.route.getPosition(timestamp - 1);
  Point otherTo = unit.route.getPosition(timestamp);
  
  if (otherTo == null) {
    if (to.x == otherAt.x && to.y == otherAt.y) continue outer;
  } else {
    if (to.x == otherTo.x && to.y == otherTo.y) continue outer;
  }
}
*/
