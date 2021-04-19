package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;

public class Test3BioArray extends BioArray {
	
	public Test3BioArray() {
		width = 7;
		height = 7;
		
		reserviorTiles.add(new Point(0, 0));
		reserviorTiles.add(new Point(width - 1, 0));
		reserviorTiles.add(new Point(0, height - 1));
		reserviorTiles.add(new Point(width - 1, height - 1));
		
	}
}
