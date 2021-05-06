package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.EmptyModuleCatalog;

public class Test2BioArray extends BioArray {
	
	public Test2BioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
		reserviorTiles.add(new Point(6, 6));
	}
}
