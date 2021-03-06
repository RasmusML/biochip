package dmb.testbench.tests;

import dmb.components.input.BioArray;
import dmb.components.module.ModuleCatalog;

public class Test6BioArray extends BioArray {
	
	public Test6BioArray() {
	  width = 14;
    height = 14;
    
    catalog = new ModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(0, height - 1, 1);
    
    catalog.registerDisposer(width - 1, height - 1);
    
    catalog.registerHeater(4, 4, 3, 3, 15, 90f);
    catalog.registerDetector(8, 9, 1, 1, 12, "metal");
    
	}
}
