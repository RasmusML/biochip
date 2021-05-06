package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;

public class ModuleBioArray1 extends BioArray {
  
  public ModuleBioArray1() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    reserviorTiles.add(new Point(0, 0));
  }
}
