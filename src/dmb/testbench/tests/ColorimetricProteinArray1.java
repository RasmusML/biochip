package dmb.testbench.tests;

import dmb.components.input.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class ColorimetricProteinArray1 extends BioArray {

  public ColorimetricProteinArray1() {
    width = 17;
    height = 17;
    
    catalog = new EmptyModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(0, 2, 1);

    catalog.registerDisposer(4, 4);
    catalog.registerDisposer(14, 12);
  }
}
