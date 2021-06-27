package dmb.testbench.tests;

import dmb.components.input.BioArray;
import dmb.components.module.ModuleCatalog;

public class InVitroArray2 extends BioArray {
  
  public InVitroArray2() {
    width = 19;
    height = 19;
    
    catalog = new ModuleCatalog();
    catalog.registerDetector(2, 0, 2, 2, 10, "in-vitro");
    
    catalog.registerDetector(width / 2, height / 2, 3, 2, 10, "in-vitro");
    
    // sugars
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(0, 2, 1);
    catalog.registerDispenser(0, 4, 1);
    
    // samples 
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(width - 1, 2, 1);
    catalog.registerDispenser(width - 1, 4, 1);
    catalog.registerDispenser(width - 1, 6, 1);
    catalog.registerDispenser(width - 1, 8, 1);
    catalog.registerDispenser(width - 1, 10, 1);
    
    
    // dispensors
    catalog.registerDisposer(4, 4);
    catalog.registerDisposer(14, 12);
    
  }
}

