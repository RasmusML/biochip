package dmb.components.prioritizer;

import dmb.algorithms.Operation;

public class CountChainPrioritizer extends ChainPrioritizer {

  @Override
  protected int getOperationCost(Operation operation) {
    return 1;
  }
}
