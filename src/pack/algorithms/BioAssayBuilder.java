package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pack.algorithms.components.UidGenerator;

public class BioAssayBuilder {
  
  private UidGenerator generator;
  private Map<Integer, Operation> idToOperation;
  
  public BioAssayBuilder() {
    generator = new UidGenerator();
    idToOperation = new HashMap<>();
  }
  
  public int createDispenseOperation(String substance) {
    int id = generator.getId();
    
    Operation operation = new Operation();
    operation.substance = substance;
    operation.id = id;
    operation.type = OperationType.Dispense;
    operation.inputs = new Operation[0];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[0];
    operation.forwarding = new Droplet[1];
    
    idToOperation.put(id, operation);
    
    return id;
  }
  
  public int createMixOperation() {
    int id = generator.getId();
    
    Operation operation = new Operation();
    operation.id = id;
    operation.type = OperationType.Mix;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[1];

    
    idToOperation.put(id, operation);
    
    return id;
  }
  
  public int createMergeOperation() {
    int id = generator.getId();
    
    Operation operation = new Operation();
    operation.id = id;
    operation.type = OperationType.Merge;
    operation.inputs = new Operation[2];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[2];
    operation.forwarding = new Droplet[1];
    
    idToOperation.put(id, operation);
    
    return id;
  }
  
  public int createSplitOperation() {
    int id = generator.getId();
    
    Operation operation = new Operation();
    operation.id = id;
    operation.type = OperationType.Split;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[2];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[2];
    
    idToOperation.put(id, operation);
    
    return id;
  }
  

  public int createModuleOperation(String module) {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.module = module;
    operation.type = OperationType.Module;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[1];
    
    idToOperation.put(id, operation);
    
    return id;
  }
  
  public void connect(int fromId, int toId) {
    Operation from = idToOperation.get(fromId);
    Operation to = idToOperation.get(toId);
    
    int toInput = ArrayUtils.getFirstEmptySlotIndex(to.inputs);
    int fromOutput = ArrayUtils.getFirstEmptySlotIndex(from.outputs);
    
    if (toInput == -1) {
      String error = String.format("all %d input operations of operation %d (%s) are occupied.", to.inputs.length, to.id, to.type);
      throw new IllegalStateException(error);
    }
    
    if (fromOutput == -1) {
      String error = String.format("all %d output operations of operation %d (%s) are occupied.", from.outputs.length, from.id, from.type);
      throw new IllegalStateException(error);
    }

    to.inputs[toInput] = from;
    from.outputs[fromOutput] = to;
  }
  
  public int getOperationCount() {
    return idToOperation.size();
  }
  
  public Operation[] getSink() {
    List<Operation> finalOperations = new ArrayList<>();
    
    for (Operation operation : idToOperation.values()) {
      for (Operation output : operation.outputs) {
        if (output == null) {
          finalOperations.add(operation);
          break;
        }
      }
    }
    
    return finalOperations.toArray(new Operation[0]);
  }
}
