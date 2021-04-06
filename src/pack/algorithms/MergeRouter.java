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
		
		List<Node> baseToRemove = new ArrayList<>();
		for (Node node : base) {
		  boolean canRun = true;
		  
		  List<Point> spawns = new ArrayList<>();
		  for (Node input : node.inputs) {
		    Point spawn = getDropletSpawn(input.substance, reserviors, spawns);
		    
		    if (spawn == null) {
		      canRun = false;
		    } else {
		      spawns.add(spawn);
		    }
		  }
		  
		  if (canRun) {
		    System.out.println(":)");

		    if ("merge".equals(node.type)) {
		      runningOperations.add(node);
		      baseToRemove.add(node);
		      
		      for (Point spawn : spawns) {
		        Route route = new Route();
		        route.path.add(spawn);
		        
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
		
		base.removeAll(baseToRemove);
		baseToRemove.clear();
		
		int timestamp = 0;
		while (runningOperations.size() > 0) {
		  List<Node> toRemove = new ArrayList<>();
		  List<Node> toAdd = new ArrayList<>();
		  
		  for (Node node : runningOperations) {
		    
		    if ("merge".equals(node.type)) {
		      NodeExtra extra = nodeIdToNodeExtra.get(node.id);
		      
		      int id0 = extra.dropletId.get(0);
		      int id1 = extra.dropletId.get(1);

		      Droplet droplet0 = getDroplet(id0, runningDroplets);
          Droplet droplet1 = getDroplet(id1, runningDroplets);
          
          if (getPosition(droplet0).x == getPosition(droplet1).x && getPosition(droplet0).y == getPosition(droplet1).y) {
            toRemove.add(node);
            
            retiredDroplets.add(getDroplet(id0, runningDroplets));
            retiredDroplets.add(getDroplet(id1, runningDroplets));
            
            removeDroplet(id0, runningDroplets);
            removeDroplet(id1, runningDroplets);
            
            extra.done = true;
            
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
                  allDroplets.addAll(spawns);
                }
              }
              
              if (canRun) {
                System.out.println(":)2");

                if ("merge".equals(child.type)) {
                  for (Point spawn : spawns) {
                    Route route = new Route();
                    route.path.add(spawn);
                    route.start = timestamp;
                    
                    Droplet droplet = new Droplet();
                    droplet.route = route;
                    droplet.id = nextDropletId;
                    nextDropletId += 1;
                    runningDroplets.add(droplet);
                    
                    NodeExtra childExtra = nodeIdToNodeExtra.get(child.id);
                    childExtra.dropletId.add(droplet.id);
                  }
                  
                  for (Node op : operationalNodes) {
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
                    
                    NodeExtra childExtra = nodeIdToNodeExtra.get(child.id);
                    childExtra.dropletId.add(droplet.id);
                  }
                  
                  toAdd.add(child);
                  
                } else if("sink".equals(child.type)){
                  System.out.println("sink...");
                } else {
                  throw new IllegalStateException("unsupported operation! " + child.type);
                }
              }
            }
          } else {  // just do a merge move
            Point move1 = getBestMergeMove(getPosition(droplet0), getPosition(droplet1), array);
            Point newPosition1 = getPosition(droplet0).copy().add(move1);
            droplet0.route.path.add(newPosition1);
            
            Point move2 = getBestMergeMove(getPosition(droplet1), getPosition(droplet0), array);
            Point newPosition2 = getPosition(droplet1).copy().add(move2);
            droplet1.route.path.add(newPosition2);
          }
		      
		    } else {
		      throw new IllegalStateException("unsupported operation!");
		    }
		  }
		  
		  runningOperations.removeAll(toRemove);
		  runningOperations.addAll(toAdd);
		  
		  timestamp += 1;
		  
	    for (Node node : base) {
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
	        System.out.println(":)");

	        if ("merge".equals(node.type)) {
	          runningOperations.add(node);
	          baseToRemove.add(node);
	          
	          for (Point spawn : spawns) {
	            Route route = new Route();
	            route.path.add(spawn);
	            
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
	    
	    base.removeAll(baseToRemove);
	    baseToRemove.clear();
		}
		
		List<Route> routes = new ArrayList<>();
		for (Droplet droplet : retiredDroplets) {
		  routes.add(droplet.route);
		}
		
		return routes;
	}
	
	private void removeDroplet(int id, List<Droplet> droplets) {
	  for (Iterator<Droplet> it = droplets.iterator(); it.hasNext();) {
      Droplet droplet = (Droplet) it.next();
      if (id == droplet.id) it.remove();
    }
  }

  private Point getPosition(Droplet droplet) {
	  return droplet.route.path.get(droplet.route.path.size()-1);
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
      positions.add(path.get(path.size()-1));
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
			System.out.printf("%s: %s\n", reservior.position, reservior.substance);
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
		
		//throw new IllegalStateException("Could not spawn droplet. The tile is occupied or substance reservior does not exist!");
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

	private boolean inside(int x, int y, int width, int height) {
		return x >= 0 && x <= width - 1 && y >= 0 && y <= height - 1;
	}

}

class Reservior {
	public Point position;
	public String substance;
}

class Droplet {
  public int id;
  public Route route;
}

class NodeExtra {
  public boolean done; // @NOTE: input nodes do not set this true currently
  public List<Integer> dropletId = new ArrayList<>();
}
