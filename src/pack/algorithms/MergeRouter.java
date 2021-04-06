package pack.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MergeRouter {

  int nextDropletId;

  public List<Route> compute(BioAssay assay, BioArray array, MixingPercentages percentages) {
    List<Reservior> reserviors = bindSubstancesToReserviors(assay, array);

    Map<Integer, NodeExtra> nodeIdToNodeExtra = new HashMap<>();
    List<Node> nodes = assay.getNodes();

    for (Node node : nodes) {
      NodeExtra extra = new NodeExtra();
      nodeIdToNodeExtra.put(node.id, extra);
    }

    List<Node> base = assay.getOperationalBase();
    List<Node> runningOperations = new ArrayList<>();

    List<Droplet> runningDroplets = new ArrayList<>();
    List<Droplet> retiredDroplets = new ArrayList<>();

    int timestamp = 0;
    while (true) {
      
      for (Iterator<Node> it = base.iterator(); it.hasNext();) {
        Node node = it.next();
        
        boolean canRun = true;
        List<Point> allDroplets = getDropletPositions(runningDroplets);
        List<Point> spawns = new ArrayList<>();
        for (Node input : node.inputs) {
          Point spawn = getDropletSpawn(input.substance, reserviors, allDroplets);

          if (spawn == null) {
            canRun = false;
          } else {
            spawns.add(spawn);
            allDroplets.add(spawn);
          }
        }

        if (canRun) {
          it.remove();
          runningOperations.add(node);
          
          System.out.printf("spawning %d (%s)\n", node.id, node.type);

          if ("merge".equals(node.type)) {

            for (Point spawn : spawns) {
              Route route = new Route();
              route.path.add(spawn);
              route.start = timestamp;
              
              Droplet droplet = new Droplet();
              droplet.route = route;
              droplet.id = nextDropletId;
              nextDropletId += 1;
              runningDroplets.add(droplet);

              NodeExtra extra = nodeIdToNodeExtra.get(node.id);
              extra.dropletId.add(droplet.id);
            }

          } else {
            throw new IllegalStateException("unsupported operation! " + node.type);
          }
        }
      }

      List<Node> queuedOperations = new ArrayList<>();

      for (Iterator<Node> it = runningOperations.iterator(); it.hasNext();) {
        Node node = it.next();
        
        NodeExtra extra = nodeIdToNodeExtra.get(node.id);

        if ("merge".equals(node.type)) {

          int id0 = extra.dropletId.get(0);
          int id1 = extra.dropletId.get(1);

          Droplet droplet0 = getDroplet(id0, runningDroplets);
          Droplet droplet1 = getDroplet(id1, runningDroplets);
          
          Point position0 = getPosition(droplet0);
          Point position1 = getPosition(droplet1);

          boolean merged = position0.x == position1.x && position0.y == position1.y;
          if (merged) {
            it.remove();

            extra.done = true;

            retiredDroplets.add(droplet0);
            runningDroplets.remove(droplet0);

            retiredDroplets.add(droplet1);
            runningDroplets.remove(droplet1);
            
            System.out.printf("completed %d (%s)\n", node.id, node.type);

          } else { // do a merge move
            
            Point move0 = getBestMergeMove(getPosition(droplet0), getPosition(droplet1), array);
            Point newPosition0 = getPosition(droplet0).copy().add(move0);
            droplet0.route.path.add(newPosition0);

            Point move1 = getBestMergeMove(getPosition(droplet1), getPosition(droplet0), array);
            Point newPosition1 = getPosition(droplet1).copy().add(move1);
            droplet1.route.path.add(newPosition1);
          }

        } else {
          throw new IllegalStateException("unsupported operation!");
        }
        
        if (extra.done) {
          for (Node child : node.outputs) {
            List<Node> inputNodes = new ArrayList<>();
            List<Node> operationalNodes = new ArrayList<>();
  
            for (Node input : child.inputs) {
              if ("input".equals(input.type)) {
                inputNodes.add(input);
              } else {
                operationalNodes.add(input);
              }
            }
  
            boolean canRun = true;
            for (Node operationalNode : operationalNodes) {
              NodeExtra opExtra = nodeIdToNodeExtra.get(operationalNode.id);
              if (!opExtra.done) canRun = false;
            }
  
            // try and spawn all input nodes.
            List<Point> allDroplets = getDropletPositions(runningDroplets);
            List<Point> spawns = new ArrayList<>();
            for (Node input : inputNodes) {
              Point spawn = getDropletSpawn(input.substance, reserviors, allDroplets);
  
              if (spawn == null) {
                canRun = false;
              } else {
                spawns.add(spawn);
                allDroplets.add(spawn);
              }
            }
  
            if (canRun) {
              System.out.printf("starting %d (%s)\n", child.id, child.type);
  
              if ("merge".equals(child.type)) {
                NodeExtra childExtra = nodeIdToNodeExtra.get(child.id);

                for (Point spawn : spawns) {
                  Route route = new Route();
                  route.path.add(spawn);
                  route.start = timestamp;
  
                  Droplet droplet = new Droplet();
                  droplet.route = route;
                  droplet.id = nextDropletId;
                  nextDropletId += 1;
                  runningDroplets.add(droplet);
  
                  childExtra.dropletId.add(droplet.id);
                }
  
                for (Node op : operationalNodes) {
                  // :forwardIndex
                  // @incomplete: assumes no splitting for now!
                  NodeExtra oldExtra = nodeIdToNodeExtra.get(op.id);
                  int oldDropletId = oldExtra.dropletId.get(0);
  
                  Point spawn = getPosition(getDroplet(oldDropletId, retiredDroplets));
                  Route route = new Route();
                  route.start = timestamp;
                  route.path.add(spawn);
  
                  Droplet droplet = new Droplet();
                  droplet.route = route;
                  droplet.id = nextDropletId;
                  nextDropletId += 1;
                  runningDroplets.add(droplet);
  
                  childExtra.dropletId.add(droplet.id);
                }
  
                queuedOperations.add(child);
  
              } else if ("sink".equals(child.type)) {
                System.out.println("sink...");
              } else {
                throw new IllegalStateException("unsupported operation! " + child.type);
              }
            }
          }
        }
      }

      runningOperations.addAll(queuedOperations);
      if (runningOperations.size() == 0 && base.size() == 0) break;
      
      timestamp += 1;

    }

    List<Route> routes = new ArrayList<>();
    for (Droplet droplet : retiredDroplets) {
      routes.add(droplet.route);
    }

    return routes;
  }

  private Point getPosition(Droplet droplet) {
    return droplet.route.path.get(droplet.route.path.size() - 1);
  }

  private Droplet getDroplet(int id, List<Droplet> droplets) {
    for (Droplet droplet : droplets) {
      if (id == droplet.id)
        return droplet;
    }
    throw new IllegalStateException("no droplet with id: " + id);
  }

  private List<Point> getDropletPositions(List<Droplet> droplets) {
    List<Point> positions = new ArrayList<>();

    for (Droplet droplet : droplets) {
      List<Point> path = droplet.route.path;
      positions.add(path.get(path.size() - 1));
    }

    return positions;
  }

  private List<Reservior> bindSubstancesToReserviors(BioAssay assay, BioArray array) {
    List<Node> inputNodes = assay.getNodesOfType("input");
    List<Point> reserviorTiles = array.reserviorTiles;

    List<String> assigned = new ArrayList<>();
    List<String> pending = new ArrayList<>();

    List<Reservior> reserviors = new ArrayList<>();

    int reserviorIndex = 0;
    int inputIndex = 0;

    while (inputIndex < inputNodes.size()) {
      Node inputNode = inputNodes.get(inputIndex);
      inputIndex += 1;

      if (assigned.contains(inputNode.substance)) {
        pending.add(inputNode.substance);
      } else {
        assigned.add(inputNode.substance);

        Point reserviorTile = reserviorTiles.get(reserviorIndex);
        reserviorIndex += 1;

        Reservior reservior = new Reservior();
        reservior.substance = inputNode.substance;
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

    for (Reservior reservior : reserviors) {
      System.out.printf("reservior %s: %s\n", reservior.position, reservior.substance);
    }

    return reserviors;
  }

  private Point getDropletSpawn(String substance, List<Reservior> reserviors, List<Point> otherDroplets) {
    outer: for (Reservior reservior : reserviors) {
      if (!reservior.substance.equals(substance))
        continue;

      for (Point other : otherDroplets) {
        if (other.x == reservior.position.x && other.y == reservior.position.y)
          continue outer;
      }

      return reservior.position.copy();
    }

    // throw new IllegalStateException("Could not spawn droplet. The tile is
    // occupied or substance reservior does not exist!");
    return null;
  }

  private Point getBestMergeMove(Point source, Point target, BioArray array) {
    List<Point> candidates = new ArrayList<>();
    candidates.add(new Point(-1, 0));
    candidates.add(new Point(1, 0));
    candidates.add(new Point(0, 1));
    candidates.add(new Point(0, -1));
    candidates.add(new Point(0, 0));

    Point best = null;
    int shortestDistance = Integer.MAX_VALUE;

    Point at = new Point();
    for (Point dt : candidates) {
      at.x = source.x + dt.x;
      at.y = source.y + dt.y;

      // @TODO: actually do the constraints!
      // @TODO: handle all droplets

      if (!inside(at.x, at.y, array.width, array.height))
        continue;

      int distance = Math.abs(at.x - target.x) + Math.abs(at.y - target.y);

      if (distance < shortestDistance) {
        shortestDistance = distance;
        best = dt;
      }
    }

    return best;
  }

  private boolean validMove(Point position, List<Point> otherDroplets, BioArray array) {

    return false;
  }

  private boolean inside(int x, int y, int width, int height) {
    return x >= 0 && x <= width - 1 && y >= 0 && y <= height - 1;
  }

}

class Reservior { // global
  public Point position;
  public String substance;
}

class Droplet { // global?
  public int id;
  public Route route;
}

class NodeExtra { // algorithm specific
  public List<Integer> dropletId = new ArrayList<>();

  public boolean done; // @NOTE: input nodes do not set this true currently
  //public int forwardIndex;  // this becomes relevant when an input is a split, as some droplets after a split may be assigned as inputs already.
}


