package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

import engine.math.MathUtils;
import pack.helpers.Assert;

public class DropletShapeSelector {
  
  public List<Point> select(Droplet droplet, int assayWidth, int assayHeight) {
    int cellsOccupied = droplet.units.size();
    
    float squareSides = (float) Math.sqrt(cellsOccupied);
    int side1 = MathUtils.ceil(squareSides);
    int side2 = MathUtils.floor(squareSides);
    
    if (side1 * side2 < cellsOccupied) side2 += 1;
    Assert.that(side1 * side2 >= cellsOccupied);
    
    List<Point> shape = fillShape(side1, side2, droplet);
    offsetShape(shape, side1, side2, assayWidth, assayHeight, droplet);
    
    return shape;
  }

  private void offsetShape(List<Point> shape, int width, int height, int assayWidth, int assayHeight, Droplet droplet) {
    Point bottomLeft = droplet.getBottomLeftPosition();

    int offsetX = (int) MathUtils.clamp(0, assayWidth - 1 - width, bottomLeft.x);
    int offsetY = (int) MathUtils.clamp(0, assayHeight - 1 - height, bottomLeft.y);
    
    for (Point at : shape) {
      at.x += offsetX;
      at.y += offsetY;
    }
  }

  private List<Point> fillShape(int side1, int side2, Droplet droplet) {
    List<Point> shape = new ArrayList<>();
    
    int left = droplet.units.size();
    for (int s1 = 0; s1 < side1; s1++) {
      for (int s2 = 0; s2 < side2; s2++) {
        int x = s1;
        int y = s2;

        shape.add(new Point(x, y));

        left -= 1;
        if (left == 0) return shape;
      }
    }
    
    throw new IllegalStateException("broken!");
  }
}
