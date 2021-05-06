package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.SmallModuleCatalog;

public class ModuleBioArray2 extends BioArray {
  
  public ModuleBioArray2() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    reserviorTiles.add(new Point(0, 0));
    reserviorTiles.add(new Point(width - 1, height - 1));
    reserviorTiles.add(new Point(3, 1));
  }
}
