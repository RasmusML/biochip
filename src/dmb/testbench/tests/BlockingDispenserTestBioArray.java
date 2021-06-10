package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class BlockingDispenserTestBioArray extends BioArray {

  public BlockingDispenserTestBioArray() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();
    catalog.registerDispenser(0, 0, 1);
  }
}
