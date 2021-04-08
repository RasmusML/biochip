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

    // @remove
    List<Droplet> temporaryDroplets = new ArrayList<>();
    {
      Route froute = new Route();
      froute.path.add(new Point(3, 0));
  
      Droplet filler = new Droplet();
      filler.id = -1;
      filler.route = froute;
      runningDroplets.add(filler);
      temporaryDroplets.add(filler);
    }

    int timestamp = 0;
    while (true) {

      for (Iterator<Node> it = base.iterator(); it.hasNext();) {
        Node node = it.next();

        boolean canRun = true;
        List<Point> dropletPositions = getDropletPositions(runningDroplets);
        List<Point> spawns = new ArrayList<>();
        for (Node input : node.inputs) {
          Point spawn = getDropletSpawn(input.substance, reserviors, dropletPositions);

          if (spawn == null) {
            canRun = false;
          } else {
            spawns.add(spawn);
            dropletPositions.add(spawn);
          }
        }

        if (canRun) {
          it.remove();
          runningOperations.add(node);

          System.out.printf("spawning %d (%s)\n", node.id, node.type);

          if ("merge".equals(node.type)) {

            for (Point spawn : spawns) {
              Droplet droplet = spawnDroplet(spawn, runningDroplets, timestamp);

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

            Point move0 = getBestMergeMove(droplet0, droplet1, runningDroplets, array);
            Point newPosition0 = getPosition(droplet0).copy().add(move0);
            droplet0.to = newPosition0;

            Point move1 = getBestMergeMove(droplet1, droplet0, runningDroplets, array);
            Point newPosition1 = getPosition(droplet1).copy().add(move1);
            droplet1.to = newPosition1;

            // System.out.println("pos0: " + newPosition0 + ", pos1: " + newPosition1);
          }

        } else {
          throw new IllegalStateException("unsupported operation!");
        }

        if (extra.done) {
          for (Node readyNode : node.outputs) {
            List<Node> inputSpawnNodes = new ArrayList<>();
            List<Node> inputOperationalNodes = new ArrayList<>();

            for (Node input : readyNode.inputs) {
              if ("input".equals(input.type)) {
                inputSpawnNodes.add(input);
              } else {
                inputOperationalNodes.add(input);
              }
            }

            boolean canRun = true;
            for (Node operationalNode : inputOperationalNodes) {
              NodeExtra opExtra = nodeIdToNodeExtra.get(operationalNode.id);
              if (!opExtra.done) canRun = false;
            }

            // try and spawn all input nodes.
            List<Point> allDroplets = getDropletPositions(runningDroplets);
            List<Point> spawns = new ArrayList<>();
            for (Node input : inputSpawnNodes) {
              Point spawn = getDropletSpawn(input.substance, reserviors, allDroplets);

              if (spawn == null) {
                canRun = false;
              } else {
                spawns.add(spawn);
                allDroplets.add(spawn);
              }
            }

            if (canRun) {
              System.out.printf("starting %d (%s)\n", readyNode.id, readyNode.type);

              NodeExtra readyNodeExtra = nodeIdToNodeExtra.get(readyNode.id);

              if ("merge".equals(readyNode.type)) {

                for (Point spawn : spawns) {
                  Route route = new Route();
                  route.start = timestamp;

                  Droplet droplet = new Droplet();
                  droplet.route = route;
                  droplet.to = spawn;
                  droplet.id = nextDropletId;
                  nextDropletId += 1;
                  runningDroplets.add(droplet);

                  readyNodeExtra.dropletId.add(droplet.id);
                }

                for (Node op : inputOperationalNodes) {
                  // :forwardIndex
                  // @incomplete: assumes no splitting for now!
                  NodeExtra prevNodeExtra = nodeIdToNodeExtra.get(op.id);
                  int prevDropletId = prevNodeExtra.dropletId.get(0);

                  Point spawn = getPosition(getDroplet(prevDropletId, retiredDroplets));
                  Route route = new Route();
                  route.start = timestamp;

                  Droplet droplet = new Droplet();
                  droplet.route = route;
                  droplet.to = spawn;
                  droplet.id = nextDropletId;
                  nextDropletId += 1;
                  runningDroplets.add(droplet);

                  readyNodeExtra.dropletId.add(droplet.id);
                }

                queuedOperations.add(readyNode);

              } else if ("sink".equals(readyNode.type)) {
                System.out.println("sink...");
              } else {
                throw new IllegalStateException("unsupported operation! " + readyNode.type);
              }
            }
          }
        }
      }

      runningOperations.addAll(queuedOperations);

      // execute the move.
      for (Droplet droplet : runningDroplets) {
        Point next = droplet.to;
        if (next == null) next = getPosition(droplet).copy();
        droplet.to = null;

        droplet.route.path.add(next);
      }

      if (runningOperations.size() == 0 && base.size() == 0) break;

      timestamp += 1;

      /*
       * if (timestamp == 10) { retiredDroplets.addAll(runningDroplets); break; }
       */

    }

    List<Route> routes = new ArrayList<>();
    for (Droplet droplet : retiredDroplets) {
      routes.add(droplet.route);
    }

    // @TODO: remove: these droplets are manually added to test stuff.
    for (Droplet droplet : temporaryDroplets) {
      routes.add(droplet.route);
    }

    return routes;
  }

  private Droplet spawnDroplet(Point spawn, List<Droplet> runningDroplets, int timestamp) {
    Route route = new Route();
    route.path.add(spawn);
    route.start = timestamp;

    Droplet droplet = new Droplet();
    droplet.route = route;
    droplet.id = nextDropletId;
    nextDropletId += 1;
    runningDroplets.add(droplet);
    return droplet;
  }

  private Point getPosition(Droplet droplet) {
    return droplet.route.path.get(droplet.route.path.size() - 1);
  }

  private Droplet getDroplet(int id, List<Droplet> droplets) {
    for (Droplet droplet : droplets) {
      if (id == droplet.id) return droplet;
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
      if (!reservior.substance.equals(substance)) continue;

      for (Point other : otherDroplets) {
        if (other.x == reservior.position.x && other.y == reservior.position.y) continue outer;
      }

      return reservior.position.copy();
    }

    // throw new IllegalStateException("Could not spawn droplet. The tile is
    // occupied or substance reservior does not exist!");
    return null;
  }

  private Point getBestMergeMove(Droplet droplet, Droplet toMerge, List<Droplet> droplets, BioArray array) {
    List<Point> candidates = new ArrayList<>();
    candidates.add(new Point(-1, 0));
    candidates.add(new Point(1, 0));
    candidates.add(new Point(0, 1));
    candidates.add(new Point(0, -1));
    candidates.add(new Point(0, 0));

    Point best = null;
    int shortestDistance = Integer.MAX_VALUE;

    Point at = getPosition(droplet);
    Point target = (toMerge.to == null) ? getPosition(toMerge) : toMerge.to;

    for (Point dt : candidates) {
      Point next = new Point();
      next.x = at.x + dt.x;
      next.y = at.y + dt.y;

      if (!inside(next.x, next.y, array.width, array.height)) continue;
      if (!validMove(droplet, next, droplets, toMerge)) continue;

      int distance = Math.abs(next.x - target.x) + Math.abs(next.y - target.y);

      if (distance < shortestDistance) {
        shortestDistance = distance;
        best = dt;
      }
    }

    return best;
  }

  public boolean validMove(Droplet droplet, Point to, List<Droplet> droplets, Droplet companion) {
    for (Droplet other : droplets) {
      if (droplet.id == other.id) continue;

      { // dynamic constraint
        Point otherAt = getPosition(other);

        int dx = Math.abs(to.x - otherAt.x);
        int dy = Math.abs(to.y - otherAt.y);

        boolean dynamicOk = dx >= 2 || dy >= 2;

        // we assume that if there is 1 spacing in "time", then the other one will do a move
        // which handles the problem through a split or merge.
        if (companion != null && companion.id == other.id) {
          dynamicOk |= (dx == 0 && dy == 0);  // split
          dynamicOk |= (dx == 1 && dy == 0) || (dx == 0 && dy == 1);  // merge
        }

        if (!dynamicOk) return false;
      }

      { // static constraint
        if (other.to != null) {
          int dx = Math.abs(to.x - other.to.x);
          int dy = Math.abs(to.y - other.to.y);

          boolean staticOk = dx >= 2 || dy >= 2;

          if (companion != null && companion.id == other.id) {
            // special case for a merge and split
            // points can't be next to each other, but they may overlap.

            // Illegal:
            // o o o o
            // o x x o
            // o o o o
            staticOk |= (companion.to.x == to.x && companion.to.y == to.y);
          }

          if (!staticOk) return false;
        }
      }
    }

    return true;
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

  public Point to; // @TODO: dedicated move-system?
}

class NodeExtra { // algorithm specific
  public List<Integer> dropletId = new ArrayList<>();

  public boolean done; // @NOTE: input nodes do not set this true currently
  // public int forwardIndex; // this becomes relevant when an input is a split,
  // as some droplets after a split may be assigned as inputs already.
}

class SplitAction {
  public Point at;
  public Point move1, move2;
}

class MoveAction {
  public Point at;
  public Point move;
}