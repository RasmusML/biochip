package pack.algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import engine.IOUtils;
import engine.ShellUtil;

public class BioAssay {

  public String name;

  public Operation sink;

  public int count;

  public void traverse(Consumer<Operation> fn, Operation sink) {
    boolean[] visited = new boolean[count];

    List<Operation> pending = new ArrayList<>();
    pending.add(sink);

    while (pending.size() > 0) {
      Operation current = pending.remove(0);

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

    traverse(operation -> routes.value += operation.inputs.size(), sink);
    return routes.value;
  }

  public int getInputCount() {
    Wrapper<Integer> inputs = new Wrapper<>();
    inputs.value = 0;

    traverse(operation -> {
      if ("input".equals(operation.type)) inputs.value += 1;
    }, sink);

    return inputs.value;
  }

  public List<String> getInputSubstances() {
    List<String> substances = new ArrayList<>();
    traverse(operation -> {
      if ("input".equals(operation.type)) substances.add(operation.substance);
    }, sink);
    return substances;
  }

  public List<Operation> getOperationsOfType(String type) {
    List<Operation> desired = new ArrayList<>();
    traverse(operation -> {
      if (type.equals(operation.type)) desired.add(operation);
    }, sink);
    return desired;
  }

  public List<Integer> getOperationIdsOfType(String type) {
    List<Integer> ids = new ArrayList<>();
    traverse(operation -> {
      if (type.equals(operation.type)) ids.add(operation.id);
    }, sink);
    return ids;
  }

  public List<Operation> getOperationalBase() {
    List<Operation> operations = new ArrayList<>();

    traverse(operation -> {
      if ("input".equals(operation.type)) return;

      for (Operation child : operation.inputs) {
        if (!"input".equals(child.type)) return;
      }

      operations.add(operation);

    }, sink);

    return operations;
  }

  public Operation getOperation(int id) {
    Wrapper<Operation> match = new Wrapper<>();
    traverse(operation -> {
      if (operation.id == id) match.value = operation;
    }, sink);
    return match.value;
  }

  public List<Integer> getOperationIds() {
    List<Integer> ids = new ArrayList<>();
    traverse(operation -> ids.add(operation.id), sink);
    return ids;
  }

  public List<Operation> getOperations() {
    List<Operation> operations = new ArrayList<>();
    traverse(operation -> operations.add(operation), sink);
    return operations;
  }

  private static class Wrapper<T> {
    public T value;
  }

  // https://stackoverflow.com/questions/19280229/graphviz-putting-a-caption-on-a-operation-in-addition-to-a-label
  public String getGraphvizGraph() {
    StringBuilder graphBuilder = new StringBuilder();

    graphBuilder.append("digraph G {\n");

    traverse(operation -> {

      String operationAttributes;
      if (operation.type.equals("input")) {
        operationAttributes = String.format("\t%d [label = \"%d - %s\"];\n", operation.id, operation.id,
            operation.substance);
      } else if (operation.type.equals("merge")) {
        operationAttributes = String.format("\t%d [label = \"%d\", fillcolor = red, style = filled];\n", operation.id,
            operation.id);
      } else if (operation.type.equals("split")) {
        operationAttributes = String.format("\t%d [label = \"%d\", fillcolor = blue, style = filled];\n", operation.id,
            operation.id);
      } else if (operation.type.equals("sink")) {
        operationAttributes = String.format("\t%d [label = \"%d\"];\n", operation.id, operation.id);
      } else {
        throw new IllegalStateException("unsupported type: " + operation.type);
      }

      graphBuilder.append(operationAttributes);

      for (Operation input : operation.inputs) {
        String edge = String.format("\t%d -> %d;\n", input.id, operation.id);
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
