package pack.testbench.tests.functionality;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class DisposeArray1 extends BioArray {

  public DisposeArray1() {
    width = 7;
    height = 7;

    catalog = new EmptyModuleCatalog();

    catalog.registerDispenser(0, 0, 1);
    // reservoirTiles.add(new Point(5, 6));

    wasteTiles.add(new Point(width - 1, height - 1));
  }
}
