package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.Droplet;
import pack.algorithms.Module;
import pack.algorithms.Move;
import pack.algorithms.Point;

public class MoveFinder {
  
  private BioConstraintsChecker checker;
  
  public MoveFinder(BioConstraintsChecker checker) {
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
  
  private List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    Point at = droplet.route.getPosition(timestamp - 1);
    Point to = new Point();
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      to.set(at).add(move.x, move.y);
      
      if (!inside(to.x, to.y, array.width, array.height)) continue;

      // skip moves which overlap modules, unless the module is the target module.
      for (Module other : modules) {
        if (other == module) continue;
        if (within(to.x, to.y, other.position.x, other.position.y, other.width, other.height)) continue outer;
      }
      
      // skip moves which does not satisfy droplet-droplet constraints.
      for (Droplet other : droplets) {
        Point otherAt = other.route.getPosition(timestamp - 1);
        Point otherTo = other.route.getPosition(timestamp);
        
        if (other.id == droplet.id) continue;
        if (mergeSibling != null && other.id == mergeSibling.id) continue;
        
        if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) continue outer;
      }
      
      // Special case for droplets which should merge with another droplet.
      if (mergeSibling != null) {
        Point siblingAt = mergeSibling.route.getPosition(timestamp - 1);
        Point siblingTo = mergeSibling.route.getPosition(timestamp);
        if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue;
      }
      
      validMoves.add(move);
    }
    
    return validMoves;
  }
  
  // @TODO: refactor
  private boolean inside(int x, int y, int width, int height) {
    return x >= 0 && x <= width - 1 && y >= 0 && y <= height - 1;
  }
  
  private boolean within(int px, int py, int x, int y, int width, int height) {
    return px <= x + width - 1 && px >= x && py <= y + height - 1 && py >= y;
  }
}
