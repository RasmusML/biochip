package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

public class ReserviorSubstanceSelector {
  
  public List<Reservior> select(BioAssay assay, BioArray array) {
    List<Operation> spawnOperations = assay.getOperations(OperationType.Spawn);
    
    // Collections.shuffle(spawnOperations); // RandomReserviorSubstanceSelector
    
    List<Point> reserviorTiles = array.reserviorTiles;

    List<String> assigned = new ArrayList<>();
    List<String> pending = new ArrayList<>();

    List<Reservior> reserviors = new ArrayList<>();

    int reserviorIndex = 0;
    int spawnIndex = 0;

    while (spawnIndex < spawnOperations.size()) {
      Operation spawnOperation = spawnOperations.get(spawnIndex);
      spawnIndex += 1;

      if (assigned.contains(spawnOperation.substance)) {
        pending.add(spawnOperation.substance);
      } else {
        assigned.add(spawnOperation.substance);

        Point reserviorTile = reserviorTiles.get(reserviorIndex);
        reserviorIndex += 1;

        Reservior reservior = new Reservior();
        reservior.substance = spawnOperation.substance;
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
