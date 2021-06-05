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
    outer: for (Move move : Move.values()) {
      
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition(timestamp - 1);
        Point to = at.copy().add(move.x, move.y);

        if (!GeometryUtil.inside(to.x, to.y, array.width, array.height)) continue outer;
      }
      
      // skip moves which overlap modules, unless the module is the target module.
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition(timestamp - 1);
        Point to = at.copy().add(move.x, move.y);
        
        for (Module other : modules) {
          if (other == targetModule) continue;
          if (GeometryUtil.inside(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) continue outer;
        }
      }
      
      // Special case for droplets which should merge with another droplet.
      if (mergeSibling != null) {
        boolean siblingMoved = mergeSibling.hasPosition(timestamp);

        if (siblingMoved) {
          
          boolean satifiesConstraints = true;
          outer2 : for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);

            for (DropletUnit siblingUnit : mergeSibling.units) {
              Point siblingAt = siblingUnit.route.getPosition(timestamp - 1);
              Point siblingTo = siblingUnit.route.getPosition(timestamp);
              
              if (!checker.satifiesConstraints(at, to, siblingAt, siblingTo)) {
                satifiesConstraints = false;
                break outer2;
              }
            }
          }
          
          boolean willMerge = false;
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);
            
            for (DropletUnit siblingUnit : mergeSibling.units) {
              Point siblingTo = siblingUnit.route.getPosition(timestamp);
              int distance = (int) MathUtils.getManhattanDistance(to.x, to.y, siblingTo.x, siblingTo.y);
              if (distance == 1) {
                willMerge = true;
                break;
              }
            }
          }

          if (!satifiesConstraints && !willMerge) continue outer;
          
        } else {
          
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = at.copy().add(move.x, move.y);

            for (DropletUnit siblingUnit : mergeSibling.units) {
              Point siblingAt = siblingUnit.route.getPosition(timestamp - 1);
              if (!checker.satifiesConstraints(at, to, siblingAt, null)) continue outer;
            }
          }
        }
      }
      
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition(timestamp - 1);
        Point to = at.copy().add(move.x, move.y);
        
        // skip moves which does not satisfy droplet-droplet constraints.
        for (Droplet other : droplets) {
          if (other.id == droplet.id) continue;
          if (mergeSibling != null && other.id == mergeSibling.id) continue;
  
          for (DropletUnit unitUnit : other.units) {
            Point otherAt = unitUnit.route.getPosition(timestamp - 1);
            Point otherTo = unitUnit.route.getPosition(timestamp);
            
            if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) continue outer;
          }
        }
      }

      // a move is only added, if the move is valid for all droplet units.
      validMoves.add(move);
    }
    
    return validMoves;
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
