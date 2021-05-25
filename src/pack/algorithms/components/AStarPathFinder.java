package pack.algorithms.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiFunction;

public class AStarPathFinder {

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

  /**
   * Generic A* search
   * 
   * Finds the path from {@code source} to {@code to}, if it exists. The shortest
   * path is only guaranteed if {@code minimumCostFunction} is admissible and
   * consistent.
   * 
   * @param source
   * @param goal
   * @param graph               - a mapping from an object to its neighbors.
   * @param stepCostFunction    - cost to arrive at neighbor.
   * @param minimumCostFunction - the minimum cost to {@code goal}.
   * 
   * @return the path from source to goal, if no path exist return an empty list.
   * 
   */
  public <T> List<T> find(T source, T goal, Map<T, List<T>> graph, BiFunction<T, T, Float> stepCostFunction,
      BiFunction<T, T, Float> minimumCostFunction) {
    Map<T, Node<T>> tToNode = new HashMap<>();
    List<T> explored = new ArrayList<>();

    PriorityQueue<T> frontier = new PriorityQueue<>((t1, t2) -> {
      Node<T> n1 = tToNode.get(t1);
      Node<T> n2 = tToNode.get(t2);

      float c1 = n1.traveled + n1.minimumCost;
      float c2 = n2.traveled + n2.minimumCost;
      return (int) (c1 - c2);
    });

    Node<T> sourceNode = Node.root(minimumCostFunction.apply(source, goal));
    tToNode.put(source, sourceNode);
    frontier.add(source);

    while (frontier.size() > 0) {
      T current = frontier.remove();

      if (current.equals(goal)) break;

      explored.add(current);

      Node<T> currentNode = tToNode.get(current);
      List<T> children = graph.get(current);
      for (T child : children) {
        if (explored.contains(child)) continue;

        float stepCost = stepCostFunction.apply(current, child);
        float newTraveled = stepCost + currentNode.traveled;

        if (frontier.contains(child)) {
          Node<T> childNode = tToNode.get(child);
          if (newTraveled < childNode.traveled) {
            childNode.parent = current;
            childNode.traveled = newTraveled;

            // Priority Queue doesn't reorder unless the object is re-inserted.
            frontier.remove(child);
            frontier.add(child);
          }
        } else {
          float minimumCost = minimumCostFunction.apply(child, goal);
          Node<T> childNode = Node.child(newTraveled, minimumCost, current);
          tToNode.put(child, childNode);
          frontier.add(child);
        }
      }
    }

    // backtrack from goal to source.
    List<T> path = new ArrayList<>();
    path.add(goal);

    Node<T> current = tToNode.get(goal);
    if (current == null) return path; // could not find a path.

    while (current.parent != null) {
      path.add(current.parent);
      current = tToNode.get(current.parent);
    }

    Collections.reverse(path);

    return path;

  }
}
