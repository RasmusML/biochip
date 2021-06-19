package dmb.testbench.tests.functionality;

import dmb.components.input.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class MergeArray1 extends BioArray {
  
  public MergeArray1() {
    width = 14;
    height = 14;
    
    catalog = new EmptyModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
  }
}
