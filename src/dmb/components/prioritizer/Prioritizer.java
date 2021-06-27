package dmb.components.prioritizer;

import dmb.algorithms.Operation;

/**
 * A heuristic to decide which operation should select it next move first.
 */

public interface Prioritizer {

  /**
   * 
   * @param o1
   * @param o2
   * @return negative value, if o1 has higher priority than o2. positive value, if
   *         o2 has high priority than o1. Zero means same priority
   */
  public int prioritize(Operation o1, Operation o2);

}
