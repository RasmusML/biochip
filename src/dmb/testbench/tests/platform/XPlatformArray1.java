package dmb.testbench.tests.platform;

import dmb.components.input.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class XPlatformArray1 extends BioArray {
  
  public XPlatformArray1() {
    width = 32;
    height = 20 / 2;
    
    catalog = new EmptyModuleCatalog();
    
    catalog.registerDispenser(width - 1 - 2, 5, 1);
    catalog.registerDisposer(width - 1, height - 1);
  }
}
