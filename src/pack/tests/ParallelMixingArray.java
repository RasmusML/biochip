package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.EmptyModuleCatalog;

public class ParallelMixingArray extends BioArray {
  
  public ParallelMixingArray() {
    width = 14;
    height = 14;
    
    catalog = new EmptyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
  }
}
