package dmb.components.shaping;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Point;
import dmb.components.BoundingBox;
import dmb.helpers.Assert;
import framework.input.Droplet;
import framework.input.DropletUnit;
import framework.math.MathUtils;

public class DropletShapeSelector {

  public DropletShape select(Droplet droplet) {
    int cellsOccupied = droplet.units.size();

    float squareSides = (float) Math.sqrt(cellsOccupied);
    int side1 = MathUtils.ceil(squareSides);
    int side2 = MathUtils.floor(squareSides);

    if (side1 * side2 < cellsOccupied) side2 += 1;
    Assert.that(side1 * side2 >= cellsOccupied);

    DropletShape dropletShape = new DropletShape();

    BoundingBox boundingBox = droplet.getBoundingBox();

    boolean alreadyOk = isAlreadyShaped(side1, side2, boundingBox.width, boundingBox.height);
    if (alreadyOk) {
      dropletShape.width = boundingBox.width;
      dropletShape.height = boundingBox.height;
      dropletShape.shape = new ArrayList<>();

      Point lowerLeft = droplet.getBottomLeftPosition();
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition();

        Point relativePosition = at.copy().sub(lowerLeft);
        dropletShape.shape.add(relativePosition);
      }

    } else {
      dropletShape.width = side1;
      dropletShape.height = side2;
      dropletShape.shape = fillShape(side1, side2, droplet);
    }

    return dropletShape;
  }

  private boolean isAlreadyShaped(int side1, int side2, int width, int height) {
    return (side1 == width && side2 == height) || (side1 == height && side2 == width);
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