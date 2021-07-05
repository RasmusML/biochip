package aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiFunction;

import dmb.algorithms.Point;
import framework.math.MathUtils;

/**
 * A* search with the time-dimension implementation.
 * The algorithm finds the shortest path between to cells.
 * Cells may become open at different times.
 */

public class AStarPathFinder {

  private BiFunction<Point, Point, Float> stepCostFunction;
  private BiFunction<Point, Point, Float> minimumCostFunction;

  public AStarPathFinder() {
    stepCostFunction = (p1, p2) -> p1.x == p2.x && p1.y == p2.y ? 1f : 2f;
    minimumCostFunction = (p1, p2) -> MathUtils.getManhattanDistance(p1.x, p1.y, p2.x, p2.y);
  }

  /**
   * A* search with the time-dimension.
   * 
   * Finds the path from {@code from} to {@code to}, if it exists. The shortest
   * path is only guaranteed if {@code minimumCostFunction} is admissible and
   * consistent. A cell is defined by a position at the time it was visited.
   * 
   * @param from
   * @param to
   * @param timestamp
   * @param maxSteps
   * 
   * @return the path from {@code from} to {@code to}, if no path exist return an
   *         empty list.
   * 
   */
  public List<Point> search(Point from, Point to, int timestamp, int maxSteps) {
    Map<PositionInTime, Node<PositionInTime>> positionInTimeToNode = new HashMap<>();
    List<PositionInTime> explored = new ArrayList<>();

    PriorityQueue<PositionInTime> frontier = new PriorityQueue<>((t1, t2) -> {
      Node<PositionInTime> n1 = positionInTimeToNode.get(t1);
      Node<PositionInTime> n2 = positionInTimeToNode.get(t2);

      float c1 = n1.traveled + n1.minimumCost;
      float c2 = n2.traveled + n2.minimumCost;
      return (int) (c1 - c2);
    });

    PositionInTime source = new PositionInTime(from, timestamp);

    Node<PositionInTime> sourceNode = Node.root(minimumCostFunction.apply(source.position, to));

    positionInTimeToNode.put(source, sourceNode);
    frontier.add(source);

    int arrivalToTarget = -1;

    while (frontier.size() > 0) {
      PositionInTime current = frontier.remove();

      if (current.position.x == to.x && current.position.y == to.y) {
        arrivalToTarget = current.timestep;

        //System.out.printf("checked %d nodes\n", current.timestep - timestamp);

        break;
      }

      explored.add(current);

      Node<PositionInTime> currentNode = positionInTimeToNode.get(current);

      int steps = current.timestep - timestamp;
      if (steps >= maxSteps) break;

      List<PositionInTime> children = new ArrayList<>();

      List<Point> moves = getMoves(current.position, current.timestep);
      for (Point move : moves) {
        Point position = new Point(current.position).add(move.x, move.y);
        PositionInTime positionInTime = new PositionInTime(position, current.timestep + 1);
        children.add(positionInTime);
      }

      for (PositionInTime child : children) {
        if (explored.contains(child)) continue;

        float stepCost = stepCostFunction.apply(current.position, child.position);
        float newTraveled = stepCost + currentNode.traveled;

        if (frontier.contains(child)) {
          Node<PositionInTime> childNode = positionInTimeToNode.get(child);
          if (newTraveled < childNode.traveled) {
            childNode.parent = current;
            childNode.traveled = newTraveled;

            // Priority Queue doesn't reorder unless the object is re-inserted.
            frontier.remove(child);
            frontier.add(child);
          }
        } else {
          float minimumCost = minimumCostFunction.apply(child.position, to);
          Node<PositionInTime> childNode = Node.child(newTraveled, minimumCost, current);
          positionInTimeToNode.put(child, childNode);
          frontier.add(child);
        }
      }
    }

    if (arrivalToTarget == -1) return null;

    // backtrack from goal to source.
    List<Point> path = new ArrayList<>();
    path.add(to);

    PositionInTime targetInTime = new PositionInTime(to, arrivalToTarget);
    Node<PositionInTime> current = positionInTimeToNode.get(targetInTime);

    while (current.parent != null) {
      path.add(current.parent.position);
      current = positionInTimeToNode.get(current.parent);
    }

    Collections.reverse(path);

    return path;

  }

  public List<Point> getMoves(Point at, int timestep) {
    throw new IllegalStateException("override me!");
  }

  private static class Node<T> {

    public T parent;
    public float minimumCost; // also known as the heuristic
    public float traveled;

    private Node() {
    }

    public static <T> Node<T> root(float minimumCost) {
      Node<T> root = new Node<>();
      root.minimumCost = minimumCost;
      root.traveled = 0;
      root.parent = null;
      return root;
    }

    public static <T> Node<T> child(float traveled, float minimumCost, T parent) {
      Node<T> child = new Node<>();
      child.minimumCost = minimumCost;
      child.traveled = traveled;
      child.parent = parent;
      return child;
    }
  }

  private static class PositionInTime {
    public Point position;
    public int timestep;

    public PositionInTime(Point position, int timestep) {
      this.position = position;
      this.timestep = timestep;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((position == null) ? 0 : position.hashCode());
      result = prime * result + timestep;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      PositionInTime other = (PositionInTime) obj;
      if (position == null) {
        if (other.position != null)
          return false;
      } else if (!position.equals(other.position))
        return false;
      if (timestep != other.timestep)
        return false;
      return true;
    }
  }
}
