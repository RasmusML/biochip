package pack.testbench.tests.functionality;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class MixArray2 extends BioArray {
  
  public MixArray2() {
    width = 14;
    height = 14;
    
    catalog = new EmptyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    reservoirTiles.add(new Point(0, height - 1));
    reservoirTiles.add(new Point(width - 1, 0));
    
    
  }
}
