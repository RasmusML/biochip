package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.EmptyModuleCatalog;

public class PCRMixingTreeArray extends BioArray {
	
	public PCRMixingTreeArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    catalog.registerDispenser(0, height - 1, 1);
	}
}
