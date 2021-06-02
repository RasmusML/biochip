package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class Test2BioArray extends BioArray {
	
	public Test2BioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
    catalog.registerDispenser(6, 6, 1);
	}
}
