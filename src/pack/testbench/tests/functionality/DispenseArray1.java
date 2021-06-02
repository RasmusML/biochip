package pack.testbench.tests.functionality;

import pack.algorithms.BioArray;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class DispenseArray1 extends BioArray {

  public DispenseArray1() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();

    catalog.registerDispenser(0, 0, 1);

    catalog.registerDisposer(width - 1, height - 1);
  }
}
