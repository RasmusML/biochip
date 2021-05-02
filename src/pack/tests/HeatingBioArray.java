package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;

public class HeatingBioArray extends BioArray {
  
  public HeatingBioArray() {
    width = 7;
    height = 7;
    
    reserviorTiles.add(new Point(0, 0));
  }
}
