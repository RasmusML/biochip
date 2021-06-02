package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class Test1BioArray extends BioArray {
	
	public Test1BioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
    catalog.registerDispenser(0, 0, 4);
    catalog.registerDispenser(5, 6, 2);
	}
}
