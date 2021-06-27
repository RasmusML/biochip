package dmb.testbench.tests;

import dmb.components.input.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class PCRMixingTreeArray2 extends BioArray {
	
	public PCRMixingTreeArray2() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    catalog.registerDispenser(0, height - 1, 1);
    
    catalog.registerDispenser(0, 2, 1);
    catalog.registerDispenser(0, 4, 1);
    catalog.registerDispenser(4, 4, 1);
    catalog.registerDispenser(3, 6, 1);
	}
}
