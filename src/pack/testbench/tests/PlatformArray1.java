package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class PlatformArray1 extends BioArray {
  
  public PlatformArray1() {
    width = 32;
    height = 20;
    
    catalog = new EmptyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(0, 3));
    reservoirTiles.add(new Point(0, 5));
    reservoirTiles.add(new Point(0, 7));
  }
}
