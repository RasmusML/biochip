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
  
  public List<Point> getPath() {
    return path;
  }
  
  public ResolveResult request(Plan plan) {
    memory.request = plan;
    memory.tryCount = 0;
    
    List<Agent> conflicting = getConflictingAgents();
    
    boolean ok = true;
    for (Agent agent : conflicting) {
      ResolveResult result = agent.resolve(plan, Phase.resolving);
      
      if (result == ResolveResult.failed) {
        ok = false;
        break;
      }
    }
    
    if (ok) {
      for (Plan committable : memory.plans) {
        Agent agent = committable.agent;
        agent.path.addAll(committable.path);
      }
    }
    
    memory.request = null;
    memory.tryCount = 0;
    memory.failedPlans.clear();
    memory.plans.clear();
    
    return ok ? ResolveResult.ok : ResolveResult.failed;
  }
  
  public int getId() {
    return id;
  }

  public ResolveResult resolve(Plan parentPlan, Phase phase) {
    if (isResolved()) return ResolveResult.ok;

    Plan myPlan = memory.getPlan(this);
    myPlan.pushDependencyLevel();
    
    int[][] distanceGrid = getDistanceGrid(memory.plans, memory.agents);
    System.out.println("distance-grid");
    print(distanceGrid);

    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;
      
      List<Plan> failedPlans = memory.getFailedPlans(this);
      List<Point> resolvedPath = getDeadlockResolvingPath(failedPlans, distanceGrid);

      if (resolvedPath == null) {
        if (phase == Phase.outposting) {  // an agent can outpost two successive times. If this happens, the the first oupost should select another outpost in the next iteration.
          memory.addFailedPlan(myPlan);
          myPlan.popDependencyLevel();
          
          return ResolveResult.failed;
        } else {
          
          // If the parentPlan is the requesters plan, then we can try to resolve the deadlock by stalling the requesters plan.
          // If the parentPlan is not the requesters plan, then this plan fails, and the parentPlan has to find another way to resolve its deadlock.
          if (parentPlan.equals(memory.request)) {  // this is a special case for when the only way to resolve the deadlock is to make the requester stall for a period of time.
            return tryWithOutposts(parentPlan, myPlan, failedPlans);
          } else {
            // if we get here, then the agent can't resolve the deadlock, because the agent asking is not the requester.
            // The agent asking this agent to resolve the deadlock, will try and find another path to resolve the deadlock.
            // so this agent doesn't undo its plan.
            return ResolveResult.failed;
          }
        }
        
      } else {

        myPlan.addToPlan(resolvedPath);
        parentPlan.addDependency(myPlan);
        
        List<Agent> conflictingAgents = getConflictingAgents();
        
        boolean ok = true;
        for (Agent agent : conflictingAgents) {
          ResolveResult result = agent.resolve(myPlan, Phase.resolving);
          if (result == ResolveResult.failed) {
            ok = false;
            break;
          }
        }
        
        if (ok) { // commit
          return ResolveResult.ok;
        } else {  // undo and try another resolving path
          memory.addFailedPlan(myPlan);
          myPlan.clearDependencyLevel();
          myPlan.undo();
        }
      }
    }
    
    Assert.that(memory.tryCount == memory.totalTries);
    
    myPlan.popDependencyLevel();
    
    return ResolveResult.failed;  // no more iterations left.
  }

  private ResolveResult tryWithOutposts(Plan parentPlan, Plan myPlan, List<Plan> failedPlans) {
    List<Point> oldParentPlanPath = parentPlan.undo();
    
    int[][] outpostDistanceGrid = getOutpostDistanceGrid(parentPlan);
    System.out.println("outpost-grid");
    print(outpostDistanceGrid);
    
    List<Point> outposts = getOutposts(outpostDistanceGrid);
    
    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;
      
      List<Point> outpostPath = getOutpostPath(outposts, failedPlans, outpostDistanceGrid);
      
      myPlan.addToPlan(outpostPath);
      parentPlan.addDependency(myPlan);
      
      List<Agent> conflictingAgents = getConflictingAgents();

      boolean ok = true;
      for (Agent agent : conflictingAgents) {
        ResolveResult result = agent.resolve(myPlan, Phase.resolving);
        if (result == ResolveResult.failed) {
          ok = false;
          break;
        }
      }
      
      if (ok) {
        
        Agent parentAgent = parentPlan.agent;
        Agent requestingAgent = memory.request.agent;
        
        if (parentAgent.equals(requestingAgent)) {  // the agent doing the request should get to get where he wants to. The rest of the agents don't care. They only move to resolve the deadlock.
          Point parentTarget = oldParentPlanPath.get(oldParentPlanPath.size() - 1);
          
          List<Point> tiles = new ArrayList<>();
          tiles.addAll(parentPlan.path);  // detouring moves
          tiles.addAll(oldParentPlanPath); // original plan
          tiles.add(parentAgent.getPosition()); // current position
          
          List<Point> path = findPath(parentAgent, parentTarget, tiles);
          
          if (path == null) {
            // undo/fail
            Assert.that(path != null, "todo!");

            return ResolveResult.failed;
          } else {
            
            parentPlan.addToPlan(path);
            parentPlan.pushDependencyLevel();
            
            conflictingAgents = parentAgent.getConflictingAgents();
            Assert.that(conflictingAgents.contains(this));
            //conflictingAgents.add(this);
            
            ok = true;
            for (Agent agent : conflictingAgents) {
              Phase agentPhase = equals(agent) ? Phase.outposting : Phase.resolving;
              ResolveResult result = agent.resolve(parentPlan, agentPhase);
              if (result == ResolveResult.failed) {
                ok = false;
                break;
              }
            }
            
            if (ok) {
              return ResolveResult.ok;
              
            } else { // try another outpost.
              memory.addFailedPlan(parentPlan);
              
              parentPlan.popDependencyLevel();
              parentPlan.undo();
            }
          }
          
        } else {
          Assert.that(false, "@todo");

          memory.addFailedPlan(myPlan);
          
          myPlan.popDependencyLevel();
          myPlan.undo();
          
          return ResolveResult.failed;  // stalling the requester will not fix the deadlock, because the requester is not at fault.
        }
          
      } else {
        Assert.that(false, "@todo!");
        
        // @TODO: restore parent, if this swapping did not resolve the deadlock. Actually, the restoring should be outside the loop construct.
        //parentPlan = oldParentPlan; // this will not work, because we just change the pointer of the object, actually change the pointer of the attributes.
        memory.addFailedPlan(myPlan);
        
        myPlan.popDependencyLevel();
        myPlan.undo();

      }
    }

    parentPlan.addToPlan(oldParentPlanPath);
    
    return ResolveResult.failed;
  }
  
  private boolean isResolved() {
    for (Plan plan : memory.plans) {
      Agent agent = plan.agent;
      List<Agent> conflicting = agent.getConflictingAgents();
      if (conflicting.size() > 0) return false;
    }
    
    return true;
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
    
    List<Plan> plans = memory.plans;
    
    AStarPathFinder pathfinder = new AStarPathFinder() {
      @Override
      public List<Point> getMoves(Point at, int timestep) {
        parentAgent.updateOccupiedTiles(prevOccupied, timestep, plans, memory.agents);
        parentAgent.updateOccupiedTiles(occupied, timestep + 1, plans, memory.agents);
        
        //print(occupied);
        
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
    List<Plan> allPlans = memory.plans;
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

  public List<Agent> getConflictingAgents() {
    List<Agent> conflicting = new ArrayList<>();

    Plan myPlan = memory.getPlan(this);
    
    List<Plan> plans = memory.plans;
    for (int i = 0; i < myPlan.path.size(); i++) {
      Point at = myPlan.path.get(i);
      
      List<Agent> pending = new ArrayList<>();
      pending.addAll(memory.agents);
      pending.remove(this);
      
      int endTime = myPlan.agent.path.size() + i;
      
      for (Plan plan : plans) {
        if (equals(plan.agent)) continue;
        if (conflicting.contains(plan.agent)) continue;
        if (plan.path.size() == 0) continue;  // @note: this only happens for the parent when doing the reverse?

        int otherEndTime = plan.agent.getPath().size() + plan.path.size();
        if (otherEndTime > endTime) continue;
        
        pending.remove(plan.agent);

        Point otherLastAt = plan.path.get(plan.path.size() - 1);
        
        if (at.x == otherLastAt.x && at.y == otherLastAt.y) {
          conflicting.add(plan.agent);
        }
      }
      
      for (Agent other : pending) {
        if (conflicting.contains(other)) continue;

        Plan otherPlan = memory.getPlan(other);
        if (otherPlan.path.size() > 0) continue;  // use the latest position of the agent, if the agent has a plan, then no need to use the last committed move.
        
        Point otherLastAt = other.getPosition();
        
        if (at.x == otherLastAt.x && at.y == otherLastAt.y) {
          conflicting.add(other);
        }
      }
    }
    
    return conflicting;
  }
  
  // outposts are consumed here.
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
    
    List<Plan> plans = memory.plans;
    for (Plan plan : plans) {
      
      if (plan.agent.equals(memory.request.agent)) {
        for (int i = 0; i <= plan.path.size() - 1; i++) { // we can't push the "root"/requester
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
    
    // @TODO: outdated :failedplans:
    for (Plan plan : failedPlans) {
      Point last = plan.path.get(plan.path.size() - 1);
      overlay[last.x][last.y] = -2;
    }
    
    List<Point> endPoints = new ArrayList<>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (overlay[x][y] == -1) continue;
        if (overlay[x][y] == -2) continue;  // @TODO: :failedplans:
        if (overlay[x][y] == 0) continue; // if agent1 moves to agent2 (this agent) and stops, then agent2 can't push agent1 again. 
        
        Point endpoint = new Point(x, y);
        endPoints.add(endpoint);
      }
    }
    
    System.out.println("endpoints:");
    for (Point endpoint : endPoints) {
      System.out.println(">" + endpoint);
    }
    System.out.println();
    
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
    if (plan.path.size() == 0) {
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
    
    //print(occupied);

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
        
        //print(occupied);
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

  public List<Point> path;
  public List<Integer> checkpoints;
  
  public List<DependencyLevel> dependencyLevels;
  //public List<Plan> dependencies;  // plans created due to this plan.
  
  public Plan() {
    path = new ArrayList<>();
    checkpoints = new ArrayList<>();
    dependencyLevels = new ArrayList<>();
    
    pushDependencyLevel();
  }
  
  public void addToPlan(List<Point> addition) {
    int checkpoint = path.size();
    
    checkpoints.add(checkpoint);
    path.addAll(addition);
  }
  
  public void pushDependencyLevel() {
    DependencyLevel level = new DependencyLevel();
    level.dependencies = new ArrayList<>();
    dependencyLevels.add(level);
  }
  
  public void clearDependencyLevel() {
    DependencyLevel level = dependencyLevels.get(dependencyLevels.size() - 1);
    
    for (Plan plan : level.dependencies) {
      plan.undo();
    }
  }
  
  public void popDependencyLevel() {
    clearDependencyLevel();
    dependencyLevels.remove(dependencyLevels.size() - 1);
  }
  
  public void addDependency(Plan plan) {
    DependencyLevel level = dependencyLevels.get(dependencyLevels.size() - 1);
    level.dependencies.add(plan);
  }
  
  public List<Point> undo() {
    int checkpoint = checkpoints.remove(checkpoints.size() - 1);
    List<Point> removed = new ArrayList<>(path.subList(checkpoint, path.size()));
    path = new ArrayList<>(path.subList(0, checkpoint));
    return removed;
  }
  
  public Plan copy() {
    Plan copy = new Plan();
    copy.agent = agent;
    copy.path.addAll(path);
    copy.dependencyLevels.addAll(dependencyLevels); // @todo: deep-copy
    return copy;
  }
}

class DependencyLevel {
  public List<Plan> dependencies;
}

class SharedAgentMemory {
  
  public int maxDepth;  // @TODO: max planning depth.
  public int numNodesToExploreInSearch;
  
  public int totalTries;
  public int tryCount;  // share the tryCount between the agents so tries doesn't explode when agents request recursively.
  
  public List<Agent> agents;
  
  public Plan request;

  public Board board;
  
  public List<Plan> failedPlans;
  public List<Plan> plans;
  
  public SharedAgentMemory(Board board) {
    this.board = board;
    
    totalTries = 100;
    numNodesToExploreInSearch = 1000;
    
    agents = new ArrayList<>();
    
    failedPlans = new ArrayList<>();
    plans = new ArrayList<>();
  }
  
  public void start() {
    for (Agent agent : agents) {
      Plan plan = new Plan();
      plan.agent = agent;
      plans.add(plan);
    }
  }
  
  public void addFailedPlan(Plan plan) {
    failedPlans.add(plan.copy());  // @TODO: deep-copy
  }
  
  public Plan getPlan(Agent agent) {
    for (Plan plan : plans) {
      if (plan.agent.equals(agent)) return plan;
    }
    throw new IllegalStateException("broken!");
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

enum Phase {
  outposting,
  resolving;
}
