package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;
import pack.algorithms.Point;
import pack.algorithms.Reservoir;
import pack.algorithms.Tags;

public class SubstanceToReservoirAssigner {
  
  public List<Reservoir> assign(BioAssay assay, BioArray array) {
    List<Operation> dispenseOperations = assay.getOperations(OperationType.dispense);
    
    // Collections.shuffle(dispenseOperations); // RandomReservoirSubstanceSelector
    
    List<Point> reservoirTiles = array.reservoirTiles;

    List<String> assigned = new ArrayList<>();
    List<String> pending = new ArrayList<>();

    List<Reservoir> reservoirs = new ArrayList<>();

    int reservoirIndex = 0;
    int dispenseIndex = 0;

    while (dispenseIndex < dispenseOperations.size()) {
      Operation dispenseOperation = dispenseOperations.get(dispenseIndex);
      dispenseIndex += 1;

      String substance = (String) dispenseOperation.attributes.get(Tags.substance);
      if (assigned.contains(substance)) {
        pending.add(substance);
      } else {
        assigned.add(substance);

        Point reservoirTile = reservoirTiles.get(reservoirIndex);
        reservoirIndex += 1;

        Reservoir reservoir = new Reservoir();
        reservoir.substance = substance;
        reservoir.position = reservoirTile.copy();
        reservoirs.add(reservoir);

        if (reservoirIndex > reservoirTiles.size()) {
          throw new IllegalStateException("not enough reservoir tiles!");
        }
      }
    }

    while (reservoirIndex < reservoirTiles.size() && pending.size() > 0) {
      String substance = pending.remove(0);

      Point reservoirTile = reservoirTiles.get(reservoirIndex);
      reservoirIndex += 1;

      Reservoir reservoir = new Reservoir();
      reservoir.substance = substance;
      reservoir.position = reservoirTile.copy();
      reservoirs.add(reservoir);
    }

    /*
    for (Reservoir reservoir : reservoirs) {
      System.out.printf("reservoir %s: %s\n", reservoir.position, reservoir.substance);
    }
    */

    return reservoirs;
  }

}
