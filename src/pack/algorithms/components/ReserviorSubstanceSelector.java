package pack.algorithms.components;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;
import pack.algorithms.Point;
import pack.algorithms.Reservior;

public class ReserviorSubstanceSelector {
  
  public List<Reservior> select(BioAssay assay, BioArray array) {
    List<Operation> dispenseOperations = assay.getOperations(OperationType.Dispense);
    
    // Collections.shuffle(dispenseOperations); // RandomReserviorSubstanceSelector
    
    List<Point> reserviorTiles = array.reserviorTiles;

    List<String> assigned = new ArrayList<>();
    List<String> pending = new ArrayList<>();

    List<Reservior> reserviors = new ArrayList<>();

    int reserviorIndex = 0;
    int dispenseIndex = 0;

    while (dispenseIndex < dispenseOperations.size()) {
      Operation dispenseOperation = dispenseOperations.get(dispenseIndex);
      dispenseIndex += 1;

      if (assigned.contains(dispenseOperation.substance)) {
        pending.add(dispenseOperation.substance);
      } else {
        assigned.add(dispenseOperation.substance);

        Point reserviorTile = reserviorTiles.get(reserviorIndex);
        reserviorIndex += 1;

        Reservior reservior = new Reservior();
        reservior.substance = dispenseOperation.substance;
        reservior.position = reserviorTile.copy();
        reserviors.add(reservior);

        if (reserviorIndex > reserviorTiles.size()) {
          throw new IllegalStateException("not enough reservior tiles!");
        }
      }
    }

    while (reserviorIndex < reserviorTiles.size() && pending.size() > 0) {
      String substance = pending.remove(0);

      Point reserviorTile = reserviorTiles.get(reserviorIndex);
      reserviorIndex += 1;

      Reservior reservior = new Reservior();
      reservior.substance = substance;
      reservior.position = reserviorTile.copy();
      reserviors.add(reservior);
    }

    /*
    for (Reservior reservior : reserviors) {
      System.out.printf("reservior %s: %s\n", reservior.position, reservior.substance);
    }
    */

    return reserviors;
  }

}
