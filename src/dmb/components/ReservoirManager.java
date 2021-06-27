package dmb.components;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.OperationType;
import dmb.algorithms.Point;
import dmb.components.input.AttributeTags;
import dmb.components.module.Module;
import dmb.components.module.ModuleAllocator;
import framework.input.Droplet;
import framework.input.DropletUnit;

/**
 * A wrapper around ModuleAllocator which handles Reservoir allocations for
 * dispense operations.
 * 
 * @see ModuleAllocator
 * @see Module
 */

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
    for (Module dispenser : dispensers) {
      if (moduleAllocator.isInUse(dispenser)) continue;
      if (reserved.contains(dispenser)) continue;

      String dispenserSubstance = (String) dispenser.attributes.get(AttributeTags.substance);
      if (dispenserSubstance.equals(substance)) {
        if (satisfiesConstraints(droplets, dispenser, timestamp)) {
          moduleAllocator.allocate(dispenser);
          reserved.add(dispenser);
          return dispenser;
        }
      }
    }

    return null;
  }

  private boolean satisfiesConstraints(List<Droplet> droplets, Module dispenser, int timestamp) {
    for (Droplet droplet : droplets) {
      for (DropletUnit unit : droplet.units) {
        Point at = unit.route.getPosition(timestamp - 1);
        Point to = unit.route.getPosition(timestamp);

        if (!checker.satifiesConstraints(dispenser.position, at, to)) return false;
      }
    }

    return true;
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
      String dispenserSubstance = (String) dispenser.attributes.get(AttributeTags.substance);
      if (dispenserSubstance.equals(substance)) count += 1;
    }

    return count;
  }
}
