package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.LargeStrictPolicyModuleCatalog;

public class ModuleBioArray3 extends BioArray {
  
  public ModuleBioArray3() {
    width = 14;
    height = 14;
    
    catalog = new LargeStrictPolicyModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    catalog.registerDispenser(8, 5, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(10, 7, 1);
    catalog.registerDispenser(8, 7, 1);
  }
}
