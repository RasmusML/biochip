package dmb.components.prioritizer;

import dmb.algorithms.Operation;

/**
 * Ignores the path lengths; all path lengths have the same priority
 */

public class RandomPrioritizer implements Prioritizer {

  @Override
  public int prioritize(Operation o1, Operation o2) {
    return 0;
  }
}
