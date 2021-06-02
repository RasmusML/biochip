package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class BlockingDispenserTestBioArray extends BioArray {

  public BlockingDispenserTestBioArray() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();
    catalog.registerDispenser(0, 0, 1);
  }
}
