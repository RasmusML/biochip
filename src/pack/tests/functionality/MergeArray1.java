package pack.tests.functionality;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.EmptyModuleCatalog;

public class MergeArray1 extends BioArray {
  
  public MergeArray1() {
    width = 14;
    height = 14;
    
    catalog = new EmptyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    
  }
}
