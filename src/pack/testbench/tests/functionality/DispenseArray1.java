package pack.testbench.tests.functionality;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class DispenseArray1 extends BioArray {

  public DispenseArray1() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();

    reservoirTiles.add(new Point(0, 0));
    // reservoirTiles.add(new Point(5, 6));

    wasteTiles.add(new Point(width - 1, height - 1));
  }
}
