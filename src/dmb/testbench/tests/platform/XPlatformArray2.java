package dmb.testbench.tests.platform;

import dmb.components.input.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class XPlatformArray2 extends BioArray {
  
  public XPlatformArray2() {
    width = 32;
    height = 20 / 2;
    
    catalog = new EmptyModuleCatalog();
    
    catalog.registerDispenser(width - 4, 3, 1);
    catalog.registerDispenser(width - 4, 8, 1);
    
    catalog.registerDisposer(width - 1, height - 1);
  }
}
