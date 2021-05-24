package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.components.BioConstraintsChecker;
import pack.algorithms.components.MixingPercentages;
import pack.algorithms.components.MoveFinder;
import pack.algorithms.components.ReservoirManager;
import pack.algorithms.components.SubstanceToReservoirAssigner;

public class TrafficRouter implements Router {

  private BioConstraintsChecker checker;
  private MoveFinder moveFinder;

  private ReservoirManager reservoirManager;

  private int timestamp;
  private int maxIterations;
  
  private List<Operation> readyOperations;
  private List<Operation> runningOperations;

  @Override
  public RoutingResult compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    checker = new BioConstraintsChecker();
    moveFinder = new MoveFinder(checker);

    SubstanceToReservoirAssigner s2rAssigner = new SubstanceToReservoirAssigner();
    List<Reservoir> reservoirs = s2rAssigner.assign(assay, array);

    reservoirManager = new ReservoirManager(reservoirs, checker);
    
    // static lanes for now
    Road road = new Road();
    buildRoad(road);
    
    readyOperations = new ArrayList<>();
    runningOperations = new ArrayList<>();
    
    boolean earlyTerminated = false;

    timestamp = 0;

    maxIterations = 1000;
    
    List<Operation> dispenseOperations = assay.getOperations(OperationType.dispense);
    readyOperations.addAll(dispenseOperations);

    while (true) {
      
      timestamp += 1;
      
      
      if (timestamp > maxIterations) {
        earlyTerminated = true;

        break;
      }
      

      int activeOperationsCount = runningOperations.size() + readyOperations.size();
      if (activeOperationsCount == 0) break;
    }

    RoutingResult result = new RoutingResult();
    result.completed = !earlyTerminated;

    return result;
  }

  private void buildRoad(Road road) {
    List<Point> tiles = road.tiles;
    tiles.add(new Point(1, 1));
    
    tiles.add(new Point(1, 2));
    tiles.add(new Point(1, 3));
    tiles.add(new Point(1, 4));
    tiles.add(new Point(1, 5));
    
    tiles.add(new Point(1, 6));

    tiles.add(new Point(2, 6));
    tiles.add(new Point(3, 6));
    tiles.add(new Point(4, 6));
    tiles.add(new Point(5, 6));
    
    tiles.add(new Point(6, 6));

    tiles.add(new Point(6, 5));
    tiles.add(new Point(6, 4));
    tiles.add(new Point(6, 3));
    tiles.add(new Point(6, 2));

    tiles.add(new Point(6, 1));

    tiles.add(new Point(5, 1));
    tiles.add(new Point(4, 1));
    tiles.add(new Point(3, 1));
    tiles.add(new Point(2, 1));
  }
}
