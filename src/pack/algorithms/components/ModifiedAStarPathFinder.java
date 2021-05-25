package pack.algorithms.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiFunction;

import engine.math.MathUtils;
import pack.algorithms.BioArray;
import pack.algorithms.Droplet;
import pack.algorithms.Module;
import pack.algorithms.Move;
import pack.algorithms.Point;

public class ModifiedAStarPathFinder {

  private BiFunction<Point, Point, Float> stepCostFunction;
  private BiFunction<Point, Point, Float> minimumCostFunction;
  
  public ModifiedAStarPathFinder() {
    BiFunction<Point, Point, Float> costFunction = (p1, p2) -> MathUtils.distance(p1.x, p1.y, p2.x, p2.y);
    stepCostFunction = costFunction;
    minimumCostFunction = costFunction;
  }

  /**
   * A* search
   * 
   * Finds the path from {@code droplet} to {@code target}, if it exists. The shortest
   * path is only guaranteed if {@code minimumCostFunction} is admissible and
   * consistent.
   * 
   * @param droplet
   * @param target
   * @param module
   * @param droplets
   * @param array
   * @param moveFinder
   * @param modules
   * @param timestamp
   * 
   * @return the path from {@code droplet} to {@code target}, if no path exist return an empty list.
   * 
   */
  public List<Point> search(Droplet droplet, Point target, Module module, List<Droplet> droplets, BioArray array, MoveFinder moveFinder, List<Module> modules, int timestamp, int maxSteps) {
    Map<PositionInTime, Node<PositionInTime>> positionInTimeToNode = new HashMap<>();
    List<PositionInTime> explored = new ArrayList<>();

    PriorityQueue<PositionInTime> frontier = new PriorityQueue<>((t1, t2) -> {
      Node<PositionInTime> n1 = positionInTimeToNode.get(t1);
      Node<PositionInTime> n2 = positionInTimeToNode.get(t2);

      float c1 = n1.traveled + n1.minimumCost;
      float c2 = n2.traveled + n2.minimumCost;
      return (int) (c1 - c2);
    });

    Point sourcePosition = droplet.route.getPosition(timestamp);
    PositionInTime source = new PositionInTime(sourcePosition, timestamp);

    Node<PositionInTime> sourceNode = Node.root(minimumCostFunction.apply(source.position, target));
    
    positionInTimeToNode.put(source, sourceNode);
    frontier.add(source);
    
    int arrivalToTarget = -1;

    while (frontier.size() > 0) {
      PositionInTime current = frontier.remove();

      if (current.position.x == target.x && current.position.y == target.y) {
        arrivalToTarget = current.timestep;
        break;
      }

      explored.add(current);

      Node<PositionInTime> currentNode = positionInTimeToNode.get(current);
      
      int steps = current.timestep - timestamp;
      if (steps >= maxSteps) break;
      
      List<PositionInTime> children = new ArrayList<>();
      List<Move> moves = moveFinder.getValidMoves(droplet.id, current.position, null, module, current.timestep, droplets, modules, array, true);
      for (Move move : moves) {
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
          float minimumCost = minimumCostFunction.apply(child.position, target);
          Node<PositionInTime> childNode = Node.child(newTraveled, minimumCost, current);
          positionInTimeToNode.put(child, childNode);
          frontier.add(child);
        }
      }
    }

    // backtrack from goal to source.
    List<Point> path = new ArrayList<>();

    if (arrivalToTarget == -1) return path;
    path.add(target);

    PositionInTime targetInTime = new PositionInTime(target, arrivalToTarget);
    Node<PositionInTime> current = positionInTimeToNode.get(targetInTime);

    while (current.parent != null) {
      path.add(current.parent.position);
      current = positionInTimeToNode.get(current.parent);
    }

    Collections.reverse(path);

    return path;

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
