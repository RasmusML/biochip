package dmb.components.prioritizer;

import dmb.algorithms.Operation;

/**
 * Counts the length of a path.
 */

public class CountChainPrioritizer extends ChainPrioritizer {

  @Override
  protected int getOperationCost(Operation operation) {
    return 1;
  }
}
