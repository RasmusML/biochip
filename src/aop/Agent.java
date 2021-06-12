package aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dmb.algorithms.Point;
import dmb.helpers.Assert;

public class Agent {

  private int id;

  private SharedAgentMemory memory;
  
  private List<Point> path;
  
  public Agent(SharedAgentMemory memory, int id, Point spawn) {
    this.id = id;
    this.memory = memory;
    
    path = new ArrayList<>();
    path.add(spawn.copy());
  }
  
  public Point getPosition(int timestep) {
    if (timestep < 0 || timestep >= path.size()) return null;
    return path.get(timestep);
  }
  
  public Point getPosition() {
    if (path.size() == 0) return null;
    return path.get(path.size() - 1);
  }
  
  public ResolveResult request(Request request, Plan plan) {
    memory.root = plan;
    memory.tryCount = 0;
    
    if (request == Request.pathing) {
      
      List<Agent> conflicting = getConflictingAgents(plan.path);
      
      boolean ok = true;
      for (Agent agent : conflicting) {
        ResolveResult result = agent.resolve(request, plan);
        
        if (result == ResolveResult.failed) {
          ok = false;
          break;
        }
      }
      
      if (ok) {
        
        List<Plan> committablePlans = new ArrayList<>();
        committablePlans.add(plan);
        
        while (committablePlans.size() > 0) {
          Plan committable = committablePlans.remove(0);
          
          Agent agent = committable.agent;
          agent.path.addAll(committable.path);
          
          committablePlans.addAll(committable.dependencies);
        }
      }
      
      memory.root = null;
      memory.failedPlans.clear();
      
      return ok ? ResolveResult.ok : ResolveResult.failed;
    }
    
    throw new IllegalStateException("broken?");
    
  }
  
  public int getId() {
    return id;
  }

  // tries to commit to a rule which satisfies the request. Rules are explicit contained here.
  public ResolveResult resolve(Request request, Plan parentPlan) {
    
    if (request == Request.pathing) {
      
      List<Plan> plans = memory.getPlans();
      List<Agent> agents = memory.agents;
      
      int[][] distanceGrid = getDistanceGrid(plans, agents);
      print(distanceGrid);

      while (memory.tryCount < memory.totalTries) {
        memory.tryCount += 1;
        
        List<Plan> failedPlans = memory.getFailedPlans(this);
        List<Point> resolvedPath = getDeadlockResolvingPath(failedPlans, distanceGrid);
  
        if (resolvedPath == null) {
          //Assert.that(false); // @remove
          
          Plan oldParentPlan = parentPlan.copy();
          
          parentPlan.path.clear();
          parentPlan.conflictingAgents.clear();
          parentPlan.dependencies.clear();
          
          int[][] outpostDistanceGrid = getOutpostDistanceGrid(parentPlan);
          print(outpostDistanceGrid);
          
          List<Point> outposts = getOutposts(outpostDistanceGrid);
          
          // @TODO: :outpost: we assume for now the first outpost solves the issue. However, we should loop here and try the next outpost.
          
          List<Point> outpostPath = getOutpostPath(outposts, failedPlans, outpostDistanceGrid);
          
          List<Agent> conflictingAgents = getConflictingAgents(outpostPath);

          Plan plan = new Plan();
          plan.agent = this;
          plan.path = outpostPath;
          plan.conflictingAgents = conflictingAgents;
          
          parentPlan.dependencies.add(plan);
          
          boolean ok = true;
          for (Agent agent : conflictingAgents) {
            
            ResolveResult result = agent.resolve(request, plan);
            if (result == ResolveResult.failed) {
              ok = false;
              break;
            }
          }
          
          Assert.that(ok, ":outpost: testFail should work on the first first outpost.");
          
          if (ok) {
            Agent parentAgent = oldParentPlan.agent;
            Agent requestingAgent = memory.root.agent;
            if (parentAgent.equals(requestingAgent)) {  // the agent doing the request should get to get where he wants to. The rest of the agents don't care. They only move to resolve the deadlock.
              Point parentTarget = oldParentPlan.path.get(oldParentPlan.path.size() - 1);
              
              List<Point> tiles = new ArrayList<>();
              tiles.addAll(parentPlan.path);  // detouring moves
              tiles.addAll(oldParentPlan.path); // original plan
              tiles.add(parentAgent.getPosition()); // current position
              
              List<Point> path = findPath(parentAgent, parentTarget, tiles);
              
              if (path == null) {
                // undo/fail
                Assert.that(path != null, "todo!");
              } else {
                
                parentPlan.path.addAll(path);
                
                conflictingAgents = parentAgent.getConflictingAgents(path);
                Assert.that(conflictingAgents.contains(this));
                //conflictingAgents.add(this);
                
                ok = true;
                for (Agent agent : conflictingAgents) {
                  ResolveResult result = agent.resolve(request, plan);
                  if (result == ResolveResult.failed) {
                    ok = false;
                    break;
                  }
                }
                
                if (ok) {
                  memory.failedPlans.removeAll(failedPlans);
                  return ResolveResult.ok;
                } else {
                  // undo
                  parentPlan.path.clear();
                  parentPlan.path.addAll(oldParentPlan.path);
                  return ResolveResult.failed;
                }
              }
            }
              
          } else {
            Assert.that(false, "@todo");
            
            // @TODO: restore parent, if this swapping did not resolve the deadlock. Actually, the restoring should be outside the loop construct.
            //parentPlan = oldParentPlan; // this will not work, because we just change the pointer of the object, actually change the pointer of the attributes.
            
            memory.failedPlans.add(plan);
            plan.dependencies.clear();
          }
          
          return ResolveResult.failed;
          
        } else {

          List<Agent> conflictingAgents = getConflictingAgents(resolvedPath);
          
          Plan plan = new Plan();
          plan.agent = this;
          plan.path = resolvedPath;
          plan.conflictingAgents = conflictingAgents;
          
          parentPlan.dependencies.add(plan);
          
          boolean ok = true;
          for (Agent agent : conflictingAgents) {
            ResolveResult result = agent.resolve(request, plan);
            if (result == ResolveResult.failed) {
              ok = false;
              break;
            }
          }
          
          if (ok) { // commit
            memory.failedPlans.removeAll(failedPlans);
            return ResolveResult.ok;
            
          } else {
            memory.failedPlans.add(plan);
            plan.dependencies.clear();
          }
        }
      }

      return ResolveResult.failed;
      
    } else {
      throw new IllegalStateException("unknown request!");
    }
  }
  

  // @todo: we can do this smarter (use less space).
  private int[][] createGrid(List<Point> possibleTiles) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] grid = new int[width][height];
    
    return grid;
  }

  private List<Point> findPath(Agent parentAgent, Point target, List<Point> tiles) {
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    moves.add(new Point(0, 0));

    int[][] grid = createGrid(tiles);
    
    for (Point tile : tiles) {
      grid[tile.x][tile.y] = 0;
    }
    
    int[][] occupied = createGrid(tiles);
    int[][] prevOccupied = createGrid(tiles);
    
    List<Plan> plans = memory.getPlans();
    
    AStarPathFinder pathfinder = new AStarPathFinder() {
      @Override
      public List<Point> getMoves(Point at, int timestep) {
        parentAgent.updateOccupiedTiles(prevOccupied, timestep, plans, memory.agents);
        parentAgent.updateOccupiedTiles(occupied, timestep + 1, plans, memory.agents);
        
        print(occupied);
        
        List<Point> validMoves = new ArrayList<>();
        for (Point move : moves) {
          Point to = at.copy().add(move.x, move.y);
          if (to.x < 0 || to.x >= occupied.length || to.y < 0 || to.y >= occupied[0].length) continue;
          if (grid[at.x][at.y] == -1) continue; // wall
          
          if (occupied[to.x][to.y] != -1) continue;
          if (prevOccupied[to.x][to.y] != -1 && prevOccupied[to.x][to.y] == occupied[at.x][at.y]) continue;  // the agents would jump through each other. Not possible.

          validMoves.add(move);
        }
        
        return validMoves;
      }
    };

    Point from = parentAgent.getPosition();
    int timestamp = parentAgent.path.size();
    
    List<Point> path = pathfinder.search(from, target, timestamp, memory.numNodesToExploreInSearch);
    if (path == null) return null;

    path.remove(0); // we already have the start position;
    
    return path;
  }

  // get all the outposts not on the parent path plan.
  private List<Point> getOutposts(int[][] outpostDistanceGrid) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    List<Point> outposts = new ArrayList<>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (!isOutpost(x, y, outpostDistanceGrid)) continue;
        
        Point endpoint = new Point(x, y);
        outposts.add(endpoint);
      }
    }
    
    return outposts;
  }
  
  private int[][] getOutpostDistanceGrid(Plan parentPlane) {
    List<Plan> allPlans = memory.getPlans();
    List<Agent> allAgents = memory.agents;
    
    List<Plan> plans = new ArrayList<>();
    for (Plan plan : allPlans) {
      if (plan.agent.equals(parentPlane.agent)) continue;
      plans.add(plan);
    }
    
    Agent parentAgent = parentPlane.agent;
    
    List<Agent> agents = new ArrayList<>();
    for (Agent agent : allAgents) {
      if (agent.equals(parentAgent)) continue;
      agents.add(agent);
    }
    
    int[][] distanceGrid = getDistanceGrid(plans, agents);
    
    return distanceGrid;
    
  }

  // @TODO: handle circular dependencies.

  private boolean isOutpost(int x, int y, int[][] overlay) {
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    Board board = memory.board;
    
    int openNeighbourTiles = 0;
    for (Point move : moves) {
      int tx = x + move.x;
      int ty = y + move.y;
      
      if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
      if (overlay[tx][ty] == -1) continue;
      openNeighbourTiles += 1;
      
    }
    
    return openNeighbourTiles >= 3;
  }

  public List<Agent> getConflictingAgents(List<Point> path) {
    List<Agent> conflicting = new ArrayList<>();

    int timestep = 1; // @TODO
    
    List<Plan> plans = memory.getPlans();
    for (int i = 0; i < path.size(); i++) {
      Point at = path.get(i);
      
      List<Agent> pending = new ArrayList<>();
      pending.addAll(memory.agents);
      pending.remove(this);
      
      int endTime = timestep + i + 1;
      
      for (Plan plan : plans) {
        if (equals(plan.agent)) continue;
        if (conflicting.contains(plan.agent)) continue;
        if (plan.path.size() == 0) continue;  // @note: this only happens for the parent when doing the reverse?

        int otherEndTime = plan.start + plan.path.size();
        if (otherEndTime > endTime) continue;
        
        pending.remove(plan.agent);

        Point otherLastAt = plan.path.get(plan.path.size() - 1);
        
        if (at.x == otherLastAt.x && at.y == otherLastAt.y) {
          conflicting.add(plan.agent);
        }
      }
      
      for (Agent other : pending) {
        if (equals(other)) continue;
        if (conflicting.contains(other)) continue;
        
        Point otherLastAt = other.getPosition();
        
        if (at.x == otherLastAt.x && at.y == otherLastAt.y) {
          conflicting.add(other);
        }
      }
    }
    
    return conflicting;
  }
  
  private List<Point> getOutpostPath(List<Point> outposts, List<Plan> failedPlans, int[][] distanceGrid) {
    while (outposts.size() > 0) {
      Point endPoint = outposts.remove(0);
      
      List<Point> path = traverse(endPoint, distanceGrid, distanceGrid);
      if (path == null) continue; // if no path exists for the endpoint try with another endpoint
      
      
      boolean alreadyFailed = false;
      for (Plan failed : failedPlans) {
        Point last = failed.path.get(failed.path.size() - 1);
        if (last.x == endPoint.x && last.y == endPoint.y) {
          alreadyFailed = true;
          break;
        }
      }
      
      if (!alreadyFailed) {
        path.remove(0); // remove the first point, because the agent is already there.
        return path;
      }
    }
    
    return null;
  }

  private List<Point> getDeadlockResolvingPath(List<Plan> failedPlans, int[][] distanceGrid) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] overlay = new int[width][height]; // no path found (yet): -1, failedEndPoint: -2, distance: x (>= 0)
    copy(distanceGrid, overlay);
    
    List<Plan> plans = memory.getPlans();
    for (Plan plan : plans) {
      
      if (plan.agent.equals(memory.root.agent)) {
        for (int i = 0; i <= plan.path.size() - 1; i++) {
          Point point = plan.path.get(i);
          overlay[point.x][point.y] = -1;
        }
      } else {
        for (int i = 0; i <= plan.path.size() - 2; i++) { // the last position is skipped, because the agent can push the other agent.
          Point point = plan.path.get(i);
          overlay[point.x][point.y] = -1;
        }
      }
    }
    
    for (Plan plan : failedPlans) {
      Point last = plan.path.get(plan.path.size() - 1);
      overlay[last.x][last.y] = -2;
    }
    
    List<Point> endPoints = new ArrayList<>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (overlay[x][y] == -1) continue;
        if (overlay[x][y] == 0) continue; // if agent1 moves to agent2 (this agent) and stops, then agent2 can't push agent1 again. 
        
        Point endpoint = new Point(x, y);
        endPoints.add(endpoint);
      }
    }

    while (endPoints.size() > 0) {
      Point endPoint = endPoints.remove(0);
      
      List<Point> longPath = traverse(endPoint, distanceGrid, overlay);
      if (longPath == null) continue; // if no path exists for the endpoint try with another endpoint
      
      List<Point> path = shortenPath(longPath, overlay);
      path.remove(0); // remove the first point, because the agent is already there.
      
      Point shortestEndpoint = path.get(path.size() - 1);
      if (overlay[shortestEndpoint.x][shortestEndpoint.y] != -2) return path; // if this path is _not_ one of the failed paths, then use it as the path, else try another endpoint.
    }
    
    return null;
  }

  private List<Point> shortenPath(List<Point> longPath, int[][] overlay) {
    List<Point> path = new ArrayList<>();
    
    for (Point point : longPath) {
      path.add(point);
      
      if (overlay[point.x][point.y] >= 1) break;  // the first safe spot has been found. Lets stop.
    }
    
    return path;
  }

  private List<Point> traverse(Point goal, int[][] distanceGrid, int[][] overlay) {
    List<Point> longPath = new ArrayList<>();
    longPath.add(goal);
    
    Point current = goal;
    while (distanceGrid[current.x][current.y] > 0) {
      Board board = memory.board;
      
      List<Point> moves = new ArrayList<>();
      moves.add(new Point(-1, 0));
      moves.add(new Point(1, 0));
      moves.add(new Point(0, 1));
      moves.add(new Point(0, -1));
      
      int cost = distanceGrid[current.x][current.y];
      
      int nextCost = cost;
      Point selectedMove = null;
      
      for (Point move : moves) {
        int tx = current.x + move.x;
        int ty = current.y + move.y;
        
        if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
        if (distanceGrid[tx][ty] == -1) continue;
        
        int newCost = distanceGrid[tx][ty];
        if (newCost < nextCost) {
          nextCost = newCost;
          selectedMove = move;
        }
      }
      
      if (selectedMove == null) return null;
      
      Point next = current.copy().add(selectedMove);

      int repeats = cost - nextCost;
      for (int i = 0; i < repeats; i++) {
        longPath.add(next.copy());
      }
      
      current = next;
    }
    
    Collections.reverse(longPath);
    
    return longPath;
  }


  private int[][] getDistanceGrid(List<Plan> plans, List<Agent> agents) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] distanceGrid = new int[width][height]; // no path found (yet): -1, distance: x (>= 0)
    
    int[][] prevOccupied = new int[width][height]; // free: -1, id: x (>= 0)
    int[][] occupied = new int[width][height];     // free: -1, id: x (>= 0)
    
    fill(occupied, -1);
    fill(distanceGrid, -1);
    
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    int timestep = path.size();

    Point at;
    
    Plan plan = memory.getPlan(this);
    if (plan == null) {
      at = path.get(timestep - 1);
    } else {
      int planStep = plan.path.size();
      
      at = plan.path.get(planStep - 1);
      timestep += planStep;
    }
    
    List<Point> pending = new ArrayList<>();
    pending.add(at);
    
    updateOccupiedTiles(prevOccupied, timestep, plans, agents);
    updateOccupiedTiles(occupied, timestep + 1, plans, agents);
    
    print(occupied);

    int left = 1;
    int childrenLeft = 0;
    
    int minDistance = 0;
    distanceGrid[at.x][at.y] = minDistance;
    
    minDistance += 1;
    
    int maxIterations = 1000;
    int iteration = 0;
    
    // flood-fill algorithm
    while (pending.size() > 0) {
      iteration += 1;
      if (iteration >= maxIterations) break;
      
      Point current = pending.remove(0);

      int neighboursNotEncountered = 0;
      
      for (Point move : moves) {
        int tx = current.x + move.x;
        int ty = current.y + move.y;
        
        if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
        if (distanceGrid[tx][ty] != -1) continue;  // already visited
        
        if (occupied[tx][ty] != -1) { // already occupied by another agent
          neighboursNotEncountered += 1;
          continue; 
        }
        
        if (prevOccupied[tx][ty] != -1 && prevOccupied[tx][ty] == occupied[current.x][current.y]) continue;  // the agents would jump through each other. Not possible.

        distanceGrid[tx][ty] = minDistance;
        
        childrenLeft += 1;
        
        Point child = new Point(tx, ty);
        pending.add(child);
      }
      
      // an agent may need to stall for some time before a tile becomes available.
      if (occupied[current.x][current.y] == -1 && neighboursNotEncountered > 0) {
        childrenLeft += 1;
        pending.add(current);
      }

      left -= 1;
      if (left == 0) {
        minDistance += 1;
        timestep += 1;
        
        left = childrenLeft;
        childrenLeft = 0;
        
        updateOccupiedTiles(prevOccupied, timestep, plans, agents);
        updateOccupiedTiles(occupied, timestep + 1, plans, agents);
        
        print(occupied);
      }
      
    }
    
    return distanceGrid;
  }
  
  // free: -1, id: x (>= 0)
  private void updateOccupiedTiles(int[][] occupied, int timestep, List<Plan> plans, List<Agent> agents) {
    fill(occupied, -1);

    for (Plan plan : plans) {
      Agent agent = plan.agent;
      if (equals(agent)) continue;
      
      int stepsCommitted = agent.path.size();
      int stepsPlanned = plan.path.size();
      int steps = stepsCommitted + stepsPlanned;
      
      if (stepsCommitted >= timestep) continue;
      if (timestep > steps) continue;
      
      int step = timestep - stepsCommitted;
      int stepIndex = step - 1;
      Point at = plan.path.get(stepIndex);  // current plan move
      
      int id = agent.getId();
      occupied[at.x][at.y] = id;
    }
    
    for (Agent agent : agents) {
      if (equals(agent)) continue;
      
      int stepsCommitted = agent.path.size();
      if (timestep > stepsCommitted) continue;
      
      int index = timestep - 1;
      Point at = agent.path.get(index);  // current committed move
      
      int id = agent.getId();
      occupied[at.x][at.y] = id;
    }
  }

  private void copy(int[][] source, int[][] target) {
    for (int x = 0; x < source.length; x++) {
      for (int y = 0; y < source[0].length; y++) {
        target[x][y] = source[x][y];
      }
    }
  }
  
  private void fill(int[][] array, int value) {
    for (int x = 0; x < array.length; x++) {
      for (int y = 0; y < array[0].length; y++) {
        array[x][y] = value;
      }
    }
  }

  private void print(int[][] array) {
    for (int x = 0; x < array.length; x++) {
      for (int y = 0; y < array[0].length; y++) {
        System.out.printf(array[x][y] + " ");
      }
      
      System.out.printf("\n");
    }
    
    System.out.printf("\n");
    
  }
}

class TimePoint {
  public int timestep;
  public Point point;
}

class Plan {
  public Agent agent;

  public int start; // @TODO
  public List<Point> path;
  public List<Agent> conflictingAgents;
  
  public List<Plan> dependencies;  // plans created due to this plan.
  
  public Plan() {
    path = new ArrayList<>();
    conflictingAgents = new ArrayList<>();
    dependencies = new ArrayList<>();
  }
  
  public Plan copy() {
    Plan copy = new Plan();
    copy.agent = agent;
    copy.path.addAll(path);
    copy.conflictingAgents.addAll(conflictingAgents);
    copy.dependencies.addAll(dependencies);
    return copy;
  }
}

class SharedAgentMemory {
  
  public int maxDepth;  // @TODO: max planning depth.
  public int numNodesToExploreInSearch;
  
  //public int timestep;  // starts at 1.
  
  public int totalTries;
  public int tryCount;  // share the tryCount between the agents so tries doesn't explode when agents request recursively.
  
  public List<Agent> agents;

  public Board board;
  
  public List<Plan> failedPlans;
  
  public Plan root;
  
  public SharedAgentMemory(Board board) {
    this.board = board;
    
    totalTries = 4;
    numNodesToExploreInSearch = 1000;
    
    agents = new ArrayList<>();
    
    failedPlans = new ArrayList<>();
  }
  
  public List<Plan> getPlans(Plan root) {
    List<Plan> plans = new ArrayList<>();
    
    List<Plan> left = new ArrayList<>();
    left.add(root);
    
    while (left.size() > 0) {
      Plan plan = left.remove(0);
      plans.add(plan);
      
      left.addAll(plan.dependencies);
    }
    
    return plans;
  }
  
  public List<Plan> getPlans() {
    Assert.that(root != null);
    return getPlans(root);
  }
  
  public void planCountSanityCheck(Agent agent) {
    List<Plan> plans = getPlans();
    int count = 0;
    for (Plan plan : plans) {
      if (plan.agent.equals(agent)) count += 1;
    }
    
    Assert.that(count <= 1, "@todo: support multiple plans (plans extending).");
  }
  
  public Plan getPlan(Agent agent) {
    planCountSanityCheck(agent);
    
    List<Plan> plans = getPlans();
    for (Plan plan : plans) {
      if (plan.agent.equals(agent)) return plan;
    }
    return null;
  }
  
  public List<Plan> getFailedPlans(Agent agent) {
    List<Plan> failed = new ArrayList<>();
    
    for (Plan plan : failedPlans) {
      if (plan.agent.equals(agent)) failed.add(plan);
    }
    
    return failed;
  }
}

enum ResolveResult {
  ok,
  failed;
}

enum Request {
  pathing;
  //eventuallyPathing;
  // moveUp
}
