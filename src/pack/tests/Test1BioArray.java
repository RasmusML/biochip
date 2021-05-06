package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;

public class Test1BioArray extends BioArray {
	
	public Test1BioArray() {
		width = 7;
		height = 7;
		
		catalog = new EmptyModuleCatalog();
		
		reserviorTiles.add(new Point(0, 0));
		reserviorTiles.add(new Point(5, 6));
	}
}
