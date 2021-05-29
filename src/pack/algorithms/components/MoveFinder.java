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
  
  private BioConstraintsChecker checker;
  
  public MoveFinder(BioConstraintsChecker checker) {
    this.checker = checker;
  }
  
  public List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(dropletUnit, droplet, null, null, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, Droplet mergeSibling, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(dropletUnit, droplet, mergeSibling, null, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(dropletUnit, droplet, null, module, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, Droplet mergeSibling, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    Point at = dropletUnit.route.getPosition(timestamp - 1);
    Point to = new Point();
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      to.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, array.width, array.height)) continue;

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

          if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue;
        }
      }
      
      // Don't let the droplet-unit of a droplet overlap. If another unit has already moved, then we check against that position.
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
      
      validMoves.add(move);
    }
    
    return validMoves;
  }
  

  /*
  public List<Move> getValidMoves(int dropletId, Point dropletPosition, Droplet mergeSibling, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array, boolean useLastKnownPositionAsNextPosition) {
    Point at = dropletPosition;
    Point to = new Point();
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      to.set(at).add(move.x, move.y);
      
      if (!GeometryUtil.inside(to.x, to.y, array.width, array.height)) continue;

      // skip moves which overlap modules, unless the module is the target module.
      for (Module other : modules) {
        if (other == module) continue;
        if (GeometryUtil.inside(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) continue outer;
      }
      
      // skip moves which does not satisfy droplet-droplet constraints.
      for (Droplet other : droplets) {
        if (other.id == dropletId) continue;
        if (mergeSibling != null && other.id == mergeSibling.id) continue;

        Point otherAt = other.route.getPosition(timestamp - 1);
        Point otherTo = other.route.getPosition(timestamp);
        
        if (useLastKnownPositionAsNextPosition) {
          if (otherAt == null) otherAt = other.route.getPosition();
          if (otherTo == null) otherTo = other.route.getPosition();
        }
        
        if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) continue outer;
      }
      
      // Special case for droplets which should merge with another droplet.
      if (mergeSibling != null) {
        Point siblingAt = mergeSibling.route.getPosition(timestamp - 1);
        Point siblingTo = mergeSibling.route.getPosition(timestamp);
        
        if (useLastKnownPositionAsNextPosition) {
          if (siblingAt == null) siblingAt = mergeSibling.route.getPosition();
          if (siblingTo == null) siblingTo = mergeSibling.route.getPosition();
        }
        
        if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue;
      }
      
      validMoves.add(move);
    }
    
    return validMoves;
  }
   */
}
