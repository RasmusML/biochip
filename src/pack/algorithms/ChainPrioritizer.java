package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

import pack.helpers.ArrayUtils;

public class ChainPrioritizer implements Prioritizer {
  
  @Override
  public int prioritize(Operation o1, Operation o2) {
    int v1 = getLongestChainValue(o1);
    int v2 = getLongestChainValue(o2);
    return -(v1 - v2);
  }

  private int getLongestChainValue(Operation operation) {
    ChainValue longest = new ChainValue();
    longest.operation = operation;
    longest.value = 0;
    
    ChainValue root = new ChainValue();
    root.operation = operation;
    root.value = 0;

    List<ChainValue> chains = new ArrayList<>();
    chains.add(root);
    
    while (chains.size() > 0) {
      ChainValue current = chains.remove(0);
      
      int successorCount = ArrayUtils.countOccupiedSlots(current.operation.outputs);
      if (successorCount == 0) {
        if (current.value > longest.value) {
          longest = current;
        }
      } else {
        int successorsLeft = successorCount;
        int index = 0;
        
        while (successorsLeft > 1) {
          Operation successor = current.operation.outputs[index];
          index += 1;

          if (successor == null) continue;
          successorsLeft -= 1;
          
          ChainValue child = new ChainValue();
          child.value = current.value + getOperationCost(successor);
          child.operation = successor;
          
          chains.add(child);
        }
        
        while (successorsLeft != 0) {
          Operation successor = current.operation.outputs[index];
          index += 1;

          if (successor == null) continue;
          successorsLeft -= 1;
          
          // re-use the object for one of children to reduce memory-usage.
          current.operation = successor;
          current.value += getOperationCost(successor);

          chains.add(current);
        }
      }
    }
    
    return longest.value;
  }
  
  private int getOperationCost(Operation operation) {
    return getAverageOperationDuration(operation);
  }
  
  private int getAverageOperationDuration(Operation operation) {
    if (operation.name.equals(OperationType.mix)) {
      return 200;
    } else if (operation.name.equals(OperationType.split)) {
      return 4;
    } else if (operation.name.equals(OperationType.dispense)) {
      return 3;
    } else if (operation.name.equals(OperationType.merge)) {
      return 20;
    } else if (operation.name.equals(OperationType.dispose)) {
      return 30;
    } else if (operation.name.equals(OperationType.heating)) {
      return 100;
    } else if (operation.name.equals(OperationType.detection)) {
      return 20;
    } else {
      throw new IllegalStateException("unknown operation type.");
    }
  }
  
  private int getCount(Operation operation) {
    return 1;
  }
  
  private int getUniform(Operation operation) {
    return 0;
  }
  
  static private class ChainValue {
    public Operation operation;
    public int value;
  }
}

