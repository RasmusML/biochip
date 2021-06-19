package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.SmallModuleCatalog;

public class ModuleBioArray1 extends BioArray {
  
  public ModuleBioArray1() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
  }
}