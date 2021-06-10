package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.SmallModuleCatalog;

public class ModuleBioArray2 extends BioArray {
  
  public ModuleBioArray2() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    catalog.registerDispenser(3, 1, 1);
  }
}
