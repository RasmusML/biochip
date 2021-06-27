package dmb.components.prioritizer;

import dmb.algorithms.Operation;
import dmb.algorithms.OperationType;

public class WeightedChainPrioritizer extends ChainPrioritizer {

  @Override
  protected int getOperationCost(Operation operation) {
    return getAverageOperationDuration(operation);
  }

  private int getAverageOperationDuration(Operation operation) {
    if (operation.name.equals(OperationType.mix)) {
      return 215;
    } else if (operation.name.equals(OperationType.split)) {
      return 4;
    } else if (operation.name.equals(OperationType.dispense)) {
      return 3;
    } else if (operation.name.equals(OperationType.merge)) {
      return 28;
    } else if (operation.name.equals(OperationType.dispose)) {
      return 42;
    } else if (operation.name.equals(OperationType.heating)) {
      return 50;
    } else if (operation.name.equals(OperationType.detection)) {
      return 50;
    } else {
      throw new IllegalStateException("unknown operation type.");
    }
  }
  
}
