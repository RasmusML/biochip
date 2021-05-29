package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

import pack.helpers.Assert;

public class Droplet {
  public int id;
  public float area;
  public List<DropletUnit> units = new ArrayList<>();
  public Operation operation;
  
  // we assume that all DropletUnits start and end at the same time for now.
  // So just get the timestamps of the first dropletunit.
  public int getStartTimestamp() {
    DropletUnit unit = units.get(0);
    return unit.route.start;
  }
  
  public int getEndTimestamp() {
    DropletUnit unit = units.get(0);
    return unit.route.start + unit.route.path.size();
  }
}

/*
class Graph<T> {
  
  public List<Node<T>> sink;
  public List<Node<T>> source;
  
  public List<Node<T>> nodes;
  
  public int size;
  
  public Graph() {
    sink = new ArrayList<>();
    source = new ArrayList<>();
    
    nodes = new ArrayList<>();
  }
  
  public void traverse(Consumer<Node<T>> fn, Node<T>... sink) {
    boolean[] visited = new boolean[size];
    
    List<Node<T>> pending = new ArrayList<>();
    for (Node<T> node : sink) {
      pending.add(node);
    }

    while (pending.size() > 0) {
      Node<T> current = pending.remove(0);

      int index = current.id;
      if (visited[index]) continue;
      visited[index] = true;

      fn.accept(current);

      for (Node<T> input : current.predecessors) {
        pending.add(input);
      }
    }
  }
}

class Node<T> {
  public Node<T>[] predecessors;
  public Node<T>[] successors;
  
  public T value;
}

class DropletGraph extends Graph<Droplet> {
  
  public DropletGraph() {
  }  
}
 */

