package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class Test1BioArray extends BioArray {
	
	public Test1BioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
		reservoirTiles.add(new Point(0, 0));
		reservoirTiles.add(new Point(5, 6));
	}
}
