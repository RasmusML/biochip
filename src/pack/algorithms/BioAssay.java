package pack.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import engine.IOUtils;
import engine.ShellUtil;

public class BioAssay {

  public String name;
  public Node sink;
  public int count;

  public void traverse(Consumer<Node> fn, Node sink) {
    boolean[] visited = new boolean[count];
    
    List<Node> pending = new ArrayList<>();
    pending.add(sink);
    
    while(pending.size() > 0) {
      Node current = pending.remove(0);
      
      int index = current.id - 1;
      if (visited[index]) continue;
      visited[index] = true;
      
      fn.accept(current);
      pending.addAll(current.inputs);

    }
  }

  public int getRouteCount() {
    Wrapper<Integer> routes = new Wrapper<>();
    routes.value = 0;

    traverse(node -> routes.value += node.inputs.size(), sink);
    return routes.value;
  }

  public int getInputCount() {
    Wrapper<Integer> inputs = new Wrapper<>();
    inputs.value = 0;

    traverse(node -> {
      if ("input".equals(node.type)) inputs.value += 1;
    }, sink);

    return inputs.value;
  }

  public List<String> getInputSubstances() {
    List<String> substances = new ArrayList<>();
    traverse(node -> {
      if ("input".equals(node.type)) substances.add(node.substance);
    }, sink);
    return substances;
  }

  public List<Node> getNodesOfType(String type) {
    List<Node> desired = new ArrayList<>();
    traverse(node -> {
      if (type.equals(node.type)) desired.add(node);
    }, sink);
    return desired;
  }

  public List<Integer> getNodeIdsOfType(String type) {
    List<Integer> ids = new ArrayList<>();
    traverse(node -> {
      if (type.equals(node.type)) ids.add(node.id);
    }, sink);
    return ids;
  }

  public List<Node> getOperationalBase() {
    List<Node> operations = new ArrayList<>();

    traverse(node -> {
      if ("input".equals(node.type)) return;

      for (Node child : node.inputs) {
        if (!"input".equals(child.type)) return;
      }

      operations.add(node);

    }, sink);

    return operations;
  }

  public Node getNode(int id) {
    Wrapper<Node> match = new Wrapper<>();
    traverse(node -> {
      if (node.id == id) match.value = node;
    }, sink);
    return match.value;
  }

  public List<Integer> getNodeIds() {
    List<Integer> ids = new ArrayList<>();
    traverse(node -> ids.add(node.id), sink);
    return ids;
  }

  public List<Node> getNodes() {
    List<Node> nodes = new ArrayList<>();
    traverse(node -> nodes.add(node), sink);
    return nodes;
  }

  private static class Wrapper<T> {
    public T value;
  }

  // https://stackoverflow.com/questions/19280229/graphviz-putting-a-caption-on-a-node-in-addition-to-a-label
  public String getGraphvizGraph() {
    StringBuilder graphBuilder = new StringBuilder();

    graphBuilder.append("digraph G {\n");

    traverse(node -> {

      String nodeAttributes;
      if (node.type.equals("input")) {
        nodeAttributes = String.format("\t%d [label = \"%d - %s\"];\n", node.id, node.id, node.substance);
      } else if (node.type.equals("merge")) {
        nodeAttributes = String.format("\t%d [label = \"%d\", fillcolor = red, style = filled];\n", node.id, node.id);
      } else if (node.type.equals("split")) {
        nodeAttributes = String.format("\t%d [label = \"%d\", fillcolor = blue, style = filled];\n", node.id, node.id);
      } else if (node.type.equals("sink")) {
        nodeAttributes = String.format("\t%d [label = \"%d\"];\n", node.id, node.id);
      } else {
        throw new IllegalStateException("unsupported type: " + node.type);
      }

      graphBuilder.append(nodeAttributes);

      for (Node input : node.inputs) {
        String edge = String.format("\t%d -> %d;\n", input.id, node.id);
        graphBuilder.append(edge);
      }

    }, sink);

    graphBuilder.append("}");

    return graphBuilder.toString();
  }

  public void saveAsPng() {
    String directory = "./assays";
    String graphPath = String.format("%s/%s.gvz", directory, name);
    String pngPath = String.format("%s/%s.png", directory, name);

    String graphString = getGraphvizGraph();
    IOUtils.writeStringToFile(graphString, graphPath);

    String graphvizPath = "C:\\Program Files (x86)\\Graphviz"; // @cleanup
    String command = String.format("\"%s\\bin\\dot.exe\" %s -Tpng -o %s", graphvizPath, graphPath, pngPath);

    int result = ShellUtil.execute(command);
    if (result != 0) throw new IllegalStateException("failed to create PNG of BioAssay");

    // remove .gvz after creating the .png
    new File(graphPath).delete();

  }
}
