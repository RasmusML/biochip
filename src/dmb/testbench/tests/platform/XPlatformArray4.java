package dmb.testbench.tests.platform;

import dmb.components.input.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class XPlatformArray4 extends BioArray {
  
  public XPlatformArray4() {
    width = 32;
    height = 20 / 2;
    
    catalog = new EmptyModuleCatalog();
    
    catalog.registerDispenser(1, 1, 1);
    catalog.registerDispenser(5, 1, 1);
    catalog.registerDispenser(9, 1, 1);
    catalog.registerDispenser(13, 1, 1);
    
    catalog.registerDetector(2, 5, 2, 2, 10, "in-vitro");
    catalog.registerDetector(10, 5, 3, 2, 10, "in-vitro");
    
    catalog.registerDisposer(width - 1, height - 1);
  }
}
