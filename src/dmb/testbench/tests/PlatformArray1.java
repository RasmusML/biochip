package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class PlatformArray1 extends BioArray {
  
  public PlatformArray1() {
    width = 32;
    height = 20;
    
    catalog = new EmptyModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(0, 3, 1);
    catalog.registerDispenser(0, 5, 1);
    catalog.registerDispenser(0, 7, 1);
  }
}
