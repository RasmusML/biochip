package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.algorithms.ModuleCatalog;

public class Test5BioArray extends BioArray {
	
	public Test5BioArray() {
	  width = 14;
    height = 14;
    
    catalog = new ModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(0, height - 1, 1);
    
    catalog.registerDisposer(width - 1, height - 1);
    
    catalog.registerHeater(4, 4, 3, 3, 15, 90f);
	}
}
