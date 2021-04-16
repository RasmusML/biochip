package pack.algorithms.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sun.corba.se.pept.encoding.InputObject;

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
      
      if (current.type.equals("input")) {
        // no input operation.
      } else if (current.type.equals("merge")) {
        MergeOperation mergeOp = (MergeOperation) current;
        pending.add(mergeOp.input1);
        pending.add(mergeOp.input2);
      } else if (current.type.equals("split")) {
        SplitOperation splitOp = (SplitOperation) current;
        pending.add(splitOp.input);
      } else if (current.type.equals("mix")) {
        MixOperation mixOp = (MixOperation) current;
        pending.add(mixOp.input);
      } else {
        throw new IllegalStateException("unknown operation: " + current.type);
      }
    }
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
      
      if (operation.type.equals("input")) {
        SpawnOperation spawnOp = (SpawnOperation) operation;
        if (!substances.contains(spawnOp.substance)) substances.add(spawnOp.substance);
      } else if (operation.type.equals("merge")) {
        MergeOperation mergeOp = (MergeOperation) operation;
      } else if (operation.type.equals("split")) {
        SplitOperation splitOp = (SplitOperation) operation;
      } else if (operation.type.equals("mix")) {
        MixOperation mixOp = (MixOperation) operation;
      } else {
        throw new IllegalStateException("unknown operation: " + operation.type);
      }
      
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
      
      if (operation.type.equals("input")) {
        SpawnOperation spawnOp = (SpawnOperation) operation;
        
      } else if (operation.type.equals("merge")) {
        MergeOperation mergeOp = (MergeOperation) operation;
        if (!mergeOp.input1.type.equals("input")) return;
        if (!mergeOp.input2.type.equals("input")) return;
        operations.add(operation);
        
      } else if (operation.type.equals("split")) {
        SplitOperation splitOp = (SplitOperation) operation;
        if (!splitOp.input.type.equals("input")) return;
        operations.add(operation);
        
      } else if (operation.type.equals("mix")) {
        MixOperation mixOp = (MixOperation) operation;
        if (!mixOp.input.type.equals("input")) return;
        operations.add(operation);
        
      } else {
        throw new IllegalStateException("unknown operation: " + operation.type);
      }
      
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
        SpawnOperation spawnOp = (SpawnOperation) operation;
        
        operationAttributes = String.format("\t%d [label = \"%d - %s\"];\n", spawnOp.id, spawnOp.id,
            spawnOp.substance);
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
      
      if (operation.type == OperationType.Spawn) {
        // no inputs.
      } else if (operation.type == OperationType.Merge) {
        MergeOperation mergeOp = (MergeOperation) operation;
        
        String edge1 = String.format("\t%d -> %d;\n", mergeOp.input1.id, operation.id);
        graphBuilder.append(edge1);
        
        String edge2 = String.format("\t%d -> %d;\n", mergeOp.input2.id, operation.id);
        graphBuilder.append(edge2);
        
      } else if (operation.type == OperationType.Split) {
        SplitOperation splitOp = (SplitOperation) operation;
        
        String edge = String.format("\t%d -> %d;\n", splitOp.input.id, operation.id);
        graphBuilder.append(edge);
        
      } else if (operation.type == OperationType.Mix) {
        MixOperation mixOp = (MixOperation) operation;
        
        String edge = String.format("\t%d -> %d;\n", mixOp.input.id, operation.id);
        graphBuilder.append(edge);
        
      } else {
        throw new IllegalStateException("unknown operation: " + operation.type);
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
