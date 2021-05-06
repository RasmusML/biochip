package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;

public class BlockingDispenserTestBioArray extends BioArray {

  public BlockingDispenserTestBioArray() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();
    
    reserviorTiles.add(new Point(0, 0));
  }
}
