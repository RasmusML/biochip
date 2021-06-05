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

public class SingleCellMoveFinder extends MoveFinder {
  
  private ConstraintsChecker checker;
  private Point to;
  
  // The behavior of single-unit droplets and multi-units droplets are different. In single-unit droplet, we assume after a merge, the two droplets occupy the same cell and not adjacent tiles. Multi-unit droplets will occupy adjacent cells.
  
  public SingleCellMoveFinder(ConstraintsChecker checker) {
    this.checker = checker;
    
    to = new Point();
  }
  
  @Override
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    Assert.that(droplet.units.size() == 1);
    DropletUnit dropletUnit = droplet.units.get(0);
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      Point at = dropletUnit.route.getPosition(timestamp - 1);
      to.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, array.width, array.height)) continue outer;

      // skip moves which overlap modules, unless the module is the target module.
      for (Module other : modules) {
        if (other == module) continue;
        if (GeometryUtil.inside(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) continue outer;
      }
      
      // Special case for droplets which should merge with another droplet.
      if (mergeSibling != null) {
        
        Assert.that(mergeSibling.units.size() == 1);
        DropletUnit unit = mergeSibling.units.get(0);
        
        Point siblingAt = unit.route.getPosition(timestamp - 1);
        Point siblingTo = unit.route.getPosition(timestamp);

        if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue outer;
      }
      
      // skip moves which does not satisfy droplet-droplet constraints.
      for (Droplet other : droplets) {
        if (other.id == droplet.id) continue;
        if (mergeSibling != null && other.id == mergeSibling.id) continue;

        for (DropletUnit unit : other.units) {
          Point otherAt = unit.route.getPosition(timestamp - 1);
          Point otherTo = unit.route.getPosition(timestamp);
          
          if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) continue outer;
        }
      }

      // a move is only added, if the move is valid for all droplet units.
      validMoves.add(move);
    }

    return validMoves;
  }
}
