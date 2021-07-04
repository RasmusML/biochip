package dmb.testbench.bundles;

import java.util.List;

import dmb.testbench.Test;

/**
 * Bundles a set of tests together.
 */

public interface TestBundle {

  public List<Test> get();

}
