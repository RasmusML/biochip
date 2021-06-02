package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.Droplet;
import pack.algorithms.DropletUnit;
import pack.algorithms.Module;
import pack.algorithms.OperationType;
import pack.algorithms.Point;
import pack.algorithms.Reservoir;
import pack.algorithms.Tags;

public class ReservoirManager {
  
  private List<Module> dispensers;
  private List<Module> reserved;
  private List<Module> commited;

  private ConstraintsChecker checker;
  private ModuleAllocator moduleAllocator;
  
  public ReservoirManager(ModuleAllocator moduleAllocator, ConstraintsChecker checker) {
    this.moduleAllocator = moduleAllocator;
    this.checker = checker;
    
    dispensers = moduleAllocator.getModulesOfOperationType(OperationType.dispense);
    reserved = new ArrayList<>();
    commited = new ArrayList<>();
  }
  
  public Module reserve(String substance, List<Droplet> droplets, int timestamp) {
    outer: for (Module dispenser : dispensers) {
      if (moduleAllocator.isInUse(dispenser)) continue;
      if (reserved.contains(dispenser)) continue;

      String dispenserSubstance = (String) dispenser.attributes.get(Tags.substance);
      if (dispenserSubstance.equals(substance)) {
        for (Droplet droplet : droplets) {
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition(timestamp - 1);
            Point to = unit.route.getPosition(timestamp);
  
            if (!checker.satifiesConstraints(dispenser.position, at, to)) continue outer;
          }
        }
        
        moduleAllocator.allocate(dispenser);
        reserved.add(dispenser);
        
        return dispenser;
      }
    }
  
    return null;
  }
  
  public void commit(Module module) {
    commited.add(module);
  }
  
  public void consumeReservations() {
    reserved.removeAll(commited);
    commited.clear();
    
    for (Module dispenser : reserved) {
      moduleAllocator.free(dispenser);
      
    }
    reserved.clear();
  }
  
  public int countReservoirsContainingSubstance(String substance) {
    int count = 0;
    for (Module dispenser : dispensers) {
      String dispenserSubstance = (String) dispenser.attributes.get(Tags.substance);
      if (dispenserSubstance.equals(substance)) count += 1;
    }
    
    return count;
  }

  public List<Reservoir> getReservoirs() {
    List<Reservoir> reservoirs = new ArrayList<>();
    for (Module dispenser : dispensers) {
      Reservoir reservoir = new Reservoir();
      reservoir.position = new Point(dispenser.position);
      
      String dispenserSubstance = (String) dispenser.attributes.get(Tags.substance);
      reservoir.substance = dispenserSubstance;
      
      reservoirs.add(reservoir);
      
    }
    
    return reservoirs;
  }
}
