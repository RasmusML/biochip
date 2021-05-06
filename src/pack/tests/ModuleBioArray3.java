package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.LargeModuleCatalog;

public class ModuleBioArray3 extends BioArray {
  
  public ModuleBioArray3() {
    width = 14;
    height = 14;
    
    catalog = new LargeModuleCatalog();
    
    reserviorTiles.add(new Point(0, 0));
    reserviorTiles.add(new Point(width - 1, height - 1));
    reserviorTiles.add(new Point(8, 5));
    reserviorTiles.add(new Point(width - 1, 0));
    reserviorTiles.add(new Point(10, 7));
    reserviorTiles.add(new Point(8, 7));
    
    
  }
}
