package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.testbench.catalogs.LargeSoftPolicyModuleCatalog;

public class PlatformArray3 extends BioArray {
  
  public PlatformArray3() {
    width = 32;
    height = 20;
    
    catalog = new LargeSoftPolicyModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    catalog.registerDispenser(8, 7, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(10, 7, 1);
    catalog.registerDispenser(4, 4, 1); // unused as enough reservoirs are available.

  }
}
