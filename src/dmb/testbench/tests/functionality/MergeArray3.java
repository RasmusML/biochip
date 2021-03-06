package dmb.testbench.tests.functionality;

import dmb.components.input.BioArray;
import dmb.components.module.ModuleCatalog;

public class MergeArray3 extends BioArray {
  
  public MergeArray3() {
    width = 11;
    height = 4;
    
    catalog = new ModuleCatalog();
    catalog.registerHeater(0, 0, 1, 1, 10, 90f);
    
    catalog.registerDispenser(4, 0, 1);
    catalog.registerDispenser(9, 0, 1);
    catalog.registerDispenser(7, 0, 1);
    
  }
}
