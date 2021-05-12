package pack.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import pack.helpers.Wrapper;

public class BioAssay {

  public String name;
  public int count;

  public Operation[] sink;

  public void traverse(Consumer<Operation> fn, Operation... sink) {
    boolean[] visited = new boolean[count];

    List<Operation> pending = new ArrayList<>();
    for (Operation operation : sink) {
      pending.add(operation);
    }

    while (pending.size() > 0) {
      Operation current = pending.remove(0);

      int index = current.id;
      if (visited[index]) continue;
      visited[index] = true;

      fn.accept(current);

      for (Operation input : current.inputs) {
        pending.add(input);
      }
    }
  }

  public void traverse(Consumer<Operation> fn) {
    traverse(fn, sink);
  }

  public int getInputCount() {
    Wrapper<Integer> inputs = new Wrapper<>();
    inputs.value = 0;

    traverse(operation -> {
      if (operation.name.equals(OperationType.dispense)) inputs.value += 1;
    });

    return inputs.value;
  }

  public List<String> getInputSubstances() {
    List<String> substances = new ArrayList<>();
    traverse(operation -> {
      if (operation.name.equals(OperationType.dispense)) {
        String substance = (String) operation.attributes.get("substance");
        substances.add(substance);
      }
    });
    return substances;
  }

  public List<Operation> getOperations(String type) {
    List<Operation> desired = new ArrayList<>();
    traverse(operation -> {
      if (operation.name.equals(type)) desired.add(operation);
    });
    return desired;
  }

  public List<Integer> getOperationIdsOfType(String type) {
    List<Integer> ids = new ArrayList<>();
    traverse(operation -> {
      if (operation.name.equals(type)) ids.add(operation.id);
    });
    return ids;
  }

  public List<Operation> getOperationalBase() {
    List<Operation> operations = new ArrayList<>();

    traverse(operation -> {
      if (operation.name.equals(OperationType.dispense)) return;

      for (Operation child : operation.inputs) {
        if (!child.name.equals(OperationType.dispense)) return;
      }

      operations.add(operation);

    });

    return operations;
  }

  public Operation getOperation(int id) {
    Wrapper<Operation> match = new Wrapper<>();
    traverse(operation -> {
      if (operation.id == id) match.value = operation;
    });
    return match.value;
  }

  public List<Integer> getOperationIds() {
    List<Integer> ids = new ArrayList<>();
    traverse(operation -> ids.add(operation.id));
    return ids;
  }

  public List<Operation> getOperations() {
    List<Operation> operations = new ArrayList<>();
    traverse(operation -> operations.add(operation));
    return operations;
  }

  // https://stackoverflow.com/questions/19280229/graphviz-putting-a-caption-on-a-operation-in-addition-to-a-label
  public String asGraphvizGraph() {
    StringBuilder graphBuilder = new StringBuilder();

    graphBuilder.append("digraph G {\n");

    traverse(operation -> {

      String operationAttributes;
      if (operation.name.equals(OperationType.dispense)) {
        String substance = (String) operation.attributes.get("substance");
        operationAttributes = String.format("\t%d [label = \"%d - %s\"];\n", operation.id, operation.id, substance);
      } else if (operation.name.equals(OperationType.merge)) {
        operationAttributes = String.format("\t%d [label = \"%d\", fillcolor = red, style = filled];\n", operation.id, operation.id);
      } else if (operation.name.equals(OperationType.split)) {
        operationAttributes = String.format("\t%d [label = \"%d\", fillcolor = blue, style = filled];\n", operation.id, operation.id);
      } else if (operation.name.equals(OperationType.mix)) {
        operationAttributes = String.format("\t%d [label = \"%d\", fillcolor = green, style = filled];\n", operation.id, operation.id);
      } else if (operation.name.equals(OperationType.heating)) {
        float temperature = (float) operation.attributes.get("temperature");
        operationAttributes = String.format("\t%d [label = \"%d - %f°C\", fillcolor = \"#FFA591\", style = filled];\n", operation.id, operation.id, temperature);
      } else {
        throw new IllegalStateException("unsupported type: " + operation.name);
      }

      graphBuilder.append(operationAttributes);

      for (Operation input : operation.inputs) {
        String edge = String.format("\t%d -> %d;\n", input.id, operation.id);
        graphBuilder.append(edge);
      }

    });

    graphBuilder.append("}");

    return graphBuilder.toString();
  }
}
