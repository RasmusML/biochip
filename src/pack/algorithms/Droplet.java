package pack.algorithms;

public class Droplet {
  public int id;

  public Route route;
  public Operation operation;

  // === MovingDroplet extends Droplet ===
  public Point to;
  public Point at;
  
  public boolean split;
  public boolean verticalSplit;
  // ===
  
  public Droplet() {
    route = new Route();
  }
  
  public Point getPosition(int timestamp) {
    int index = timestamp - route.start;
    if (index < 0 || index >= route.path.size()) return null;
    return route.path.get(index);
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

