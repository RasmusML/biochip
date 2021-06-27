package dmb.components.prioritizer;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Operation;
import dmb.helpers.ArrayUtils;

public abstract class ChainPrioritizer implements Prioritizer {
  
  protected abstract int getOperationCost(Operation operation);

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
  
  
  private class ChainValue {
    public Operation operation;
    public int value;
  }
}

