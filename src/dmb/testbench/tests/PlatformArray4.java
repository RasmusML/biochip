package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.LargeSoftPolicyModuleCatalog;

public class PlatformArray4 extends BioArray {
  
  public PlatformArray4() {
    width = 32;
    height = 20;
    
    catalog = new LargeSoftPolicyModuleCatalog();
    
    catalog.registerDispenser(6, 3, 1);
    catalog.registerDispenser(6, 5, 1);
    catalog.registerDispenser(6, 7, 1);
    catalog.registerDispenser(8, 7, 1);
    catalog.registerDispenser(10, 5, 1);
    catalog.registerDispenser(10, 7, 1);
    catalog.registerDispenser(8, 5, 1);
    catalog.registerDispenser(10, 3, 1);
    catalog.registerDispenser(12, 7, 1);
    catalog.registerDispenser(8, 3, 1);
    catalog.registerDispenser(10, 9, 1);
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(20, 14, 1);
    
    catalog.registerDisposer(0, height - 1);
  }
}
