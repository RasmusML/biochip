package dmb.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dmb.algorithms.Operation;
import dmb.algorithms.OperationType;
import dmb.components.input.AttributeTags;
import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.module.Module;
import dmb.components.module.ModuleAllocator;
import dmb.helpers.Assert;
import dmb.helpers.RandomUtil;

/**
 * Selects the fluid substance of an arrays reservoirs based on an assay.
 */

public class SubstanceToReservoirAssigner {

  public void assign(BioAssay assay, BioArray array, ModuleAllocator moduleAllocator) {
    List<Operation> dispenseOperations = assay.getOperations(OperationType.dispense);

    Collections.shuffle(dispenseOperations, RandomUtil.get());

    List<Module> dispensers = moduleAllocator.getModulesOfOperationType(OperationType.dispense);

    List<String> assigned = new ArrayList<>();
    List<String> pending = new ArrayList<>();

    int reservoirIndex = 0;
    int dispenseIndex = 0;

    while (dispenseIndex < dispenseOperations.size()) {
      Operation dispenseOperation = dispenseOperations.get(dispenseIndex);
      dispenseIndex += 1;

      String substance = (String) dispenseOperation.attributes.get(AttributeTags.substance);
      if (assigned.contains(substance)) {
        // if a reservoir containing the substance already exists, then only only assign
        // leftover reservoirs to the substance.  
        pending.add(substance);
      } else {
        Assert.that(reservoirIndex < dispensers.size(), "no reservoirs left to assign substance to! Add more reservoirs in the array");

        assigned.add(substance);

        Module dispenser = dispensers.get(reservoirIndex);
        dispenser.attributes.put(AttributeTags.substance, substance);
        reservoirIndex += 1;
      }
    }

    // assign leftover reservoirs to substances which occur multiple times.
    while (reservoirIndex < dispensers.size() && pending.size() > 0) {
      String substance = pending.remove(0);

      Module dispenser = dispensers.get(reservoirIndex);
      dispenser.attributes.put(AttributeTags.substance, substance);
      reservoirIndex += 1;
    }
  }
}
