package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.Droplet;
import pack.algorithms.Move;
import pack.algorithms.Point;

public class MoveFinder {
  
  private BioConstraintsChecker checker;
  
  public MoveFinder(BioConstraintsChecker checker) {
    this.checker = checker;
  }
  
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, int timestamp, List<Droplet> droplets, BioArray array) {
    Point at = droplet.route.getPosition(timestamp - 1);
    Point to = new Point();
    
    List<Move> validMoves = new ArrayList<>();
    outer: for (Move move : Move.values()) {
      to.set(at).add(move.x, move.y);
      
      if (!inside(to.x, to.y, array.width, array.height)) continue;

      for (Droplet other : droplets) {
        Point otherAt = other.route.getPosition(timestamp - 1);
        Point otherTo = other.route.getPosition(timestamp);
        
        if (other.id == droplet.id) continue;
        if (mergeSibling != null && other.id == mergeSibling.id) continue;
        
        if (!checker.satifiesConstraints(at, to, otherAt, otherTo)) continue outer;
      }
      
      if (mergeSibling != null) {
        Point siblingAt = mergeSibling.route.getPosition(timestamp - 1);
        Point siblingTo = mergeSibling.route.getPosition(timestamp);
        if (!checker.satisfiesCompanionConstraints(at, to, siblingAt, siblingTo)) continue;
      }
      
      validMoves.add(move);
    }
    
    return validMoves;
  }
  
  private boolean inside(int x, int y, int width, int height) {
    return x >= 0 && x <= width - 1 && y >= 0 && y <= height - 1;
  }
}
