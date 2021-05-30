package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.Droplet;
import pack.algorithms.DropletUnit;
import pack.algorithms.Point;
import pack.algorithms.Reservoir;
import pack.helpers.Assert;

public class ReservoirManager {
  
  private List<Reservoir> reservoirs;
  private List<Reservoir> reserved;

  private BioConstraintsChecker checker;
  
  public ReservoirManager(List<Reservoir> reservoirs, BioConstraintsChecker checker) {
    this.reservoirs = reservoirs;
    this.checker = checker;
    
    reserved = new ArrayList<>();
  }
  
  public Reservoir reserve(String substance, List<Droplet> droplets, int timestamp) {
    outer: for (Reservoir reservoir : reservoirs) {
      if (reserved.contains(reservoir)) continue;

      if (reservoir.substance.equals(substance)) {
        
        for (Droplet droplet : droplets) {
          
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = unit.route.getPosition(timestamp);
  
            if (!checker.satifiesConstraints(reservoir.position, at, to)) continue outer;
          }
        }
        
        reserved.add(reservoir);
        
        return reservoir;
      }
    }
  
    return null;
  }
  
  public void consumeReservations() {
    reserved.clear();
  }
  
  public int countReservoirsContainingSubstance(String substance) {
    int count = 0;
    for (Reservoir reservoir : reservoirs) {
      if (reservoir.substance.equals(substance)) count += 1;
    }
    return count;
  }

  public List<Reservoir> getReservoirs() {
    return reservoirs;
  }
}
