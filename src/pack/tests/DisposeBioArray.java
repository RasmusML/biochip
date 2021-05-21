package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.EmptyModuleCatalog;

public class DisposeBioArray extends BioArray {
	
	public DisposeBioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
		reservoirTiles.add(new Point(0, 0));
		//reservoirTiles.add(new Point(5, 6));
	}
}