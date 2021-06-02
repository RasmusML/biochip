package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.testbench.catalogs.SmallModuleCatalog;

public class ModuleBioArray1 extends BioArray {
  
  public ModuleBioArray1() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
  }
}
