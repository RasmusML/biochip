package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.Droplet;
import pack.algorithms.DropletUnit;
import pack.algorithms.Module;
import pack.algorithms.Move;
import pack.algorithms.Point;
import pack.helpers.GeometryUtil;

public class MoveFinder {
  
  private ConstraintsChecker checker;
  
  public MoveFinder(ConstraintsChecker checker) {
    this.checker = checker;
  }
  
  public List<Move> getValidMoves(Droplet droplet, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, null, null, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, mergeSibling, null, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(Droplet droplet, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, null, module, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    Point to = new Point();
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      
      for (DropletUnit dropletUnit : droplet.units) {
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
          
          for (DropletUnit unit : mergeSibling.units) {
            Point siblingAt = unit.route.getPosition(timestamp - 1);
            Point siblingTo = unit.route.getPosition(timestamp);
  
            Point siblingPosition = (siblingTo == null) ? siblingAt : siblingTo;
            if (to.x == siblingPosition.x && to.y == siblingPosition.y) continue outer;
          }
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
        
      }

      // a move is only added, if the move is valid for all droplet units.
      validMoves.add(move);
    }
    
    return validMoves;
  }
  
  public List<Move> getValidMovesSingleUnitDroplets(Droplet droplet, Droplet mergeSibling, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    Point to = new Point();
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      
      for (DropletUnit dropletUnit : droplet.units) {
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
          
          for (DropletUnit unit : mergeSibling.units) {
            Point siblingAt = unit.route.getPosition(timestamp - 1);
            Point siblingTo = unit.route.getPosition(timestamp);
  
            if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue outer;
          }
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
        
      }

      // a move is only added, if the move is valid for all droplet units.
      validMoves.add(move);
    }
    
    return validMoves;
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
}
