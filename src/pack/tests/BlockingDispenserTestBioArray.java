package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.EmptyModuleCatalog;

public class BlockingDispenserTestBioArray extends BioArray {

  public BlockingDispenserTestBioArray() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
  }
}
