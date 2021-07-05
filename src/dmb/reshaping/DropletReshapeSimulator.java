package dmb.reshaping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmb.algorithms.Point;
import dmb.components.Droplet;
import dmb.components.DropletUnit;
import dmb.components.moves.Move;
import dmb.helpers.GeometryUtil;
import framework.math.MathUtils;

public class DropletReshapeSimulator {

  private int width, height;

  private boolean[][] targetShape;
  private int[][] currentShape; // ids

  private Droplet droplet;
  private List<Point> shape;

  private Comparator<DropletUnit> firstIterationSorter;
  private Comparator<DropletUnit> secondIterationSorter;

  private Map<DropletUnit, Point> pointToTarget;
  private Map<DropletUnit, Integer> pointToMinOutsidePointDistance;

  public DropletReshapeSimulator(int width, int height) {
    this.width = width;
    this.height = height;

    firstIterationSorter = (p1, p2) -> {
      Point p1At = p1.route.getPosition();
      Point p2At = p2.route.getPosition();

      if (targetShape[p1At.x][p1At.y] && targetShape[p2At.x][p2At.y]) {
        int d1 = pointToMinOutsidePointDistance.get(p1);
        int d2 = pointToMinOutsidePointDistance.get(p2);
        return d1 - d2;
      } else {
        int v1 = targetShape[p1At.x][p1At.y] ? 1 : -1;
        int v2 = targetShape[p2At.x][p2At.y] ? 1 : -1;
        return v1 - v2;
      }
    };

    secondIterationSorter = (p1, p2) -> {
      Point t1 = pointToTarget.get(p1);
      Point t2 = pointToTarget.get(p2);

      Point p1At = p1.route.getPosition();
      Point p2At = p2.route.getPosition();

      int d1 = (int) MathUtils.getManhattanDistance(p1At.x, p1At.y, t1.x, t1.y);
      int d2 = (int) MathUtils.getManhattanDistance(p2At.x, p2At.y, t2.x, t2.y);

      return d1 - d2;
    };
  }

  public void reshape(Droplet droplet, List<Point> shape) {
    this.droplet = droplet;
    this.shape = shape;

    currentShape = new int[width][height];
    targetShape = new boolean[width][height];

    for (int i = 0; i < droplet.units.size(); i++) {
      DropletUnit unit = droplet.units.get(i);

      Point tile = unit.route.getPosition();
      int id = i + 1;

      currentShape[tile.x][tile.y] = id;
    }

    for (Point point : shape) {
      targetShape[point.x][point.y] = true;
    }
  }

  public void step() {
    pointToTarget = new HashMap<>();
    pointToMinOutsidePointDistance = new HashMap<>();

    for (DropletUnit unit : droplet.units) {
      Point at = unit.route.getPosition();

      int minDistance = Integer.MAX_VALUE;
      Point closestTarget = null;

      for (Point target : shape) {
        int distance = (int) MathUtils.getManhattanDistance(at.x, at.y, target.x, target.y);

        if (distance < minDistance) {
          minDistance = distance;
          closestTarget = target;
        }
      }

      pointToTarget.put(unit, closestTarget);
    }

    List<DropletUnit> left = new ArrayList<>(droplet.units);

    List<DropletUnit> inside = new ArrayList<>();
    List<DropletUnit> outside = new ArrayList<>();

    for (DropletUnit unit : left) {
      Point at = unit.route.getPosition();

      if (targetShape[at.x][at.y]) {
        inside.add(unit);
      } else {
        outside.add(unit);
      }
    }

    boolean anyMove = true;
    while (anyMove) {

      pointToMinOutsidePointDistance.clear();

      for (DropletUnit insider : inside) {
        Point insiderAt = insider.route.getPosition();

        int minDistance = Integer.MAX_VALUE;
        for (DropletUnit outsider : outside) {
          Point outsiderAt = outsider.route.getPosition();

          int distance = (int) MathUtils.getManhattanDistance(insiderAt.x, insiderAt.y, outsiderAt.x, outsiderAt.y);

          if (distance < minDistance) {
            minDistance = distance;
          }
        }

        pointToMinOutsidePointDistance.put(insider, minDistance);
      }

      left.sort(firstIterationSorter);

      anyMove = false;
      for (DropletUnit unit : left) {
        Point at = unit.route.getPosition();

        Move move = getFillingMove(at);

        if (move != null) {
          left.remove(unit);
          updatePosition(unit, move);

          if (targetShape[at.x][at.y]) {
            inside.add(unit);
            outside.remove(unit);
          }

          anyMove = true;

          break;
        }
      }
    }

    // points not moved are either not inside the target shape or they "stuck" within the shape.
    // the points not within the target shape move those closer.

    left.sort(secondIterationSorter);

    for (DropletUnit unit : left) {
      Point at = unit.route.getPosition();

      Move move = Move.None;
      if (!targetShape[at.x][at.y]) move = getNonFillingMove(unit);

      updatePosition(unit, move);
    }
  }

  private void updatePosition(DropletUnit unit, Move move) {
    Point at = unit.route.getPosition();
    Point to = at.copy().add(move.x, move.y);
    unit.route.path.add(to);

    int id = currentShape[at.x][at.y];
    currentShape[at.x][at.y] = 0;
    currentShape[to.x][to.y] = id;
  }

  private Move getFillingMove(Point point) {
    Point target = new Point();

    Move[] moves = Move.values();
    for (Move move : moves) {
      if (move == Move.None) continue;

      target.set(point).add(move.x, move.y);

      if (!GeometryUtil.inside(target.x, target.y, 0, 0, width, height)) continue;

      if (targetShape[target.x][target.y] && currentShape[target.x][target.y] == 0) {
        return move;
      }
    }

    return null;
  }

  private Move getNonFillingMove(DropletUnit unit) {
    Point at = unit.route.getPosition();

    Point target = pointToTarget.get(unit);
    Point to = new Point();

    int minDistance = Integer.MAX_VALUE;
    Move selected = null;

    Move[] moves = Move.values();
    for (Move move : moves) {
      to.set(at).add(move.x, move.y);

      if (!GeometryUtil.inside(to.x, to.y, 0, 0, width, height)) continue;
      if (move != Move.None && currentShape[to.x][to.y] != 0) continue;

      int distance = (int) MathUtils.getManhattanDistance(to.x, to.y, target.x, target.y);

      if (distance < minDistance) {
        minDistance = distance;
        selected = move;
      }
    }

    return selected;
  }
}
