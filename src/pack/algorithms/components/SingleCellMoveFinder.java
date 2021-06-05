package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.Droplet;
import pack.algorithms.DropletUnit;
import pack.algorithms.Module;
import pack.algorithms.Move;
import pack.algorithms.MoveFinder;
import pack.algorithms.Point;
import pack.helpers.Assert;
import pack.helpers.GeometryUtil;

/**
 * SingleCellMoveFinder finds the valid moves for a droplet which can only occupy a single cell.
 * MultiCellMoveFinder assumes two droplets merge if they occupy the same cell.
 * 
 * @author Rasmus
 *
 */

public class SingleCellMoveFinder extends MoveFinder {
  
  private ConstraintsChecker checker;
  
  public SingleCellMoveFinder(ConstraintsChecker checker) {
    this.checker = checker;
  }
  
  @Override
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    Assert.that(droplet.units.size() == 1);
    DropletUnit unit = droplet.units.get(0);
    
    List<Move> validMoves = new ArrayList<>();
    
    for (Move move : Move.values()) {
      Point at = unit.route.getPosition(timestamp - 1);
      Point to = at.copy().add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, array.width, array.height)) continue;

      // skip moves which overlap modules, unless the module is the target module.
      if (isWithinModule(at, to, targetModule, modules)) continue;
      
      // Special case for droplets which should merge with another droplet.
      if (mergeSibling != null) {
        
        Assert.that(mergeSibling.units.size() == 1);
        DropletUnit siblingUnit = mergeSibling.units.get(0);
        
        Point siblingAt = siblingUnit.route.getPosition(timestamp - 1);
        Point siblingTo = siblingUnit.route.getPosition(timestamp);

        if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue;
      }
      
      // skip moves which does not satisfy droplet-droplet constraints.
      if (!satisfiesDropletDropletConstraints(at, to, droplet, mergeSibling, droplets, timestamp)) continue;

      // a move is only added, if the move is valid for all droplet units.
      validMoves.add(move);
    }

    return validMoves;
  }
  
  private boolean satisfiesDropletDropletConstraints(Point at, Point to, Droplet droplet, Droplet mergeSibling, List<Droplet> droplets, int timestamp) {
    for (Droplet other : droplets) {
      Assert.that(other.units.size() == 1);
      DropletUnit otherUnit = other.units.get(0);
      
      if (other.id == droplet.id) continue;
      if (mergeSibling != null && other.id == mergeSibling.id) continue;
      
      Point otherAt = otherUnit.route.getPosition(timestamp - 1);
      Point otherTo = otherUnit.route.getPosition(timestamp);
      
      if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) return false;
    }
    
    return true;
  }
  
  private boolean isWithinModule(Point at, Point to, Module module, List<Module> modules) {
    for (Module other : modules) {
      if (other == module) continue;
      if (GeometryUtil.inside(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) return true;
    }
    
    return false;
  }
}
