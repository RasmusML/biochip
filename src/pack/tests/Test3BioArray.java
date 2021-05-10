package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.EmptyModuleCatalog;

public class Test3BioArray extends BioArray {
	
	public Test3BioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
		reservoirTiles.add(new Point(0, 0));
		reservoirTiles.add(new Point(width - 1, 0));
		reservoirTiles.add(new Point(0, height - 1));
		reservoirTiles.add(new Point(width - 1, height - 1));
		
	}
}
