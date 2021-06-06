package pack.algorithms.components;

import pack.algorithms.Point;

public class ConstraintsChecker {
  
  public boolean satifiesConstraints(Point to0, Point at1, Point to1) {
    // dynamic constraint
    if (!satisfiesSpacingConstraint(to0, at1)) return false;
    
    // static constraint
    if (!satisfiesSpacingConstraint(to0, to1)) return false;
    
    return true;
  }
  
  public boolean satifiesConstraints(Point at0, Point to0, Point at1, Point to1) {
    // dynamic constraint
    if (!satisfiesSpacingConstraint(to0, at1)) return false;
    if (!satisfiesSpacingConstraint(to1, at0)) return false;
    
    // static constraint
    if (!satisfiesSpacingConstraint(to0, to1)) return false;
    
    return true;
  }

  public boolean satisfiesSpacingConstraint(Point p1, Point p2) {
    return satisfiesSpacingConstraint(p1, p2, 2);
  }
  
  public boolean satisfiesSpacingConstraint(Point p1, Point p2, int spacing) {
    // If 1 or more points are null, then we assume that those points are not placed. Thus, the placement is valid, because the points do not interfere. @docs
    if (p1 == null || p2 == null) return true;  
    
    int dx = Math.abs(p1.x - p2.x);
    int dy = Math.abs(p1.y - p2.y);
    
    return dx >= spacing || dy >= spacing;
  }
  
  // @TODO: cleanup the functions below are only for single unit droplets. @Refactor

  public boolean satisfiesCompanionConstraints(Point to0, Point at1, Point to1) {
    if (satifiesConstraints(to0, at1, to1)) return true;
    if (!satisfiesDynamicCompanionConstraint(to0, at1)) return false;
    if (!satifiesStaticCompanionConstraint(to0, to1)) return false;
    return true;
  }
  
  
  public boolean satisfiesCompanionConstraints(Point at0, Point to0, Point at1, Point to1) {
    if (satifiesConstraints(at0, to0, at1, to1)) return true;
    if (!satisfiesDynamicCompanionConstraint(to0, at1)) return false;
    if (!satisfiesDynamicCompanionConstraint(to1, at0)) return false;
    if (!satifiesStaticCompanionConstraint(to0, to1)) return false;
    return true;
  }

  
  private boolean satifiesStaticCompanionConstraint(Point to0, Point to1) {
    if (to0 == null || to1 == null) return true;
    
    // special case for a merge and split points can't be next to each other, but they may overlap.
    
    // Illegal:
    // o o o o
    // o x x o
    // o o o o
    int dx = Math.abs(to0.x - to1.x);
    int dy = Math.abs(to0.y - to1.y);
    
    boolean staticOk = dx == 0 && dy == 0;
    if (!staticOk) return false;
    
    return true;
  }

  private boolean satisfiesDynamicCompanionConstraint(Point to0, Point at1) {
    if (to0 != null && at1 != null) {
      int dx = Math.abs(to0.x - at1.x);
      int dy = Math.abs(to0.y - at1.y);
      
      // we assume that if there is 1 spacing in "time", then the other one will do a move which handles the problem through a split or merge.
      boolean dynamicOk = (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
      if (!dynamicOk) return false;
    }
    
    return true;
  }
  
}
