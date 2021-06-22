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
      Plan childPlan = memory.getPlan(agent);
      plan.addDependency(childPlan);
      
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

    plan.popDependencyLevel();
    
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
    
    ResolveResult result;
    
    // try finding a safe cell.
    result = tryWithResolvingPaths(parentPlan);
    if (result == ResolveResult.ok) return ResolveResult.ok;
      
    // try pushing the parent back.
    //result = tryWithPushingParentBack(parentPlan);  // @incomplete
    //if (result == ResolveResult.ok) return ResolveResult.ok;
    
    // try stalling the requester.
    result = tryWithOutposts(parentPlan, phase);
    if (result == ResolveResult.ok) return ResolveResult.ok;
    
    memory.addFailedPlan(myPlan);
    
    myPlan.undoDependencyLevel();
    myPlan.popDependencyLevel();
    
    return ResolveResult.failed;  // no more iterations left.
  }
  
  private ResolveResult tryWithPushingParentBack(Plan parentPlan) {
    Plan myPlan = memory.getPlan(this);
    
    Point at = myPlan.getPosition();
    if (at == null) at = myPlan.agent.getPosition();
    
    Point pushBack = at.copy();
    
    int time = path.size() + myPlan.path.size() + 1;  // next timestep.
    List<Out> outs = getOuts(at, time);
    
    while (memory.tryCount < memory.totalTries) {
      for (Out out : outs) {
        memory.tryCount += 1;

        // @TODO: remove out from list.
        if (out.timestep == -1) continue; // is not possible to stay at the "out", because another agent needs the cell.
        
        time = getNextPushTime(time);
        
        List<Point> stays = new ArrayList<>();
        int stayBy = time - out.timestep;
        for (int i = 0; i < stayBy; i++) {
          stays.add(out.at);
        }
        
        List<Point> addedPath = new ArrayList<>();
        addedPath.addAll(stays);
        addedPath.add(pushBack);
        
        myPlan.addToPlan(addedPath);
        
        out.timestep = time;
        
        ResolveResult result = resolve(myPlan, Phase.resolving);  // @TODO: Phase.pushingParentBack? flag to say that you can't push the parent back. Actually, in some cases they can move back and forth to resolve the deadlock.
        if (result == ResolveResult.ok) return ResolveResult.ok;
        
        myPlan.undo();
      }
    }
    
    return ResolveResult.failed;
  }

  private List<Out> getOuts(Point at, int timestep) {
    List<Out> outs = new ArrayList<>();

    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    for (Point move : moves) {
      Point outAt = at.copy().add(move);
      
      if (isOccupied(outAt, timestep)) continue;
      
      Out out = new Out();
      out.at = outAt;
      out.timestep = timestep;
      
      outs.add(out);
    }
    
    return outs;
  }
  
  private boolean isOccupied(Point at, int timestep) {
    
    return false;
  }
  
  private int getNextPushTime(int time) {
    // @TODO: use isOccupied for simple implementation getNextPushTime (no detour)
    return -1;
  }

  class Out {
    public Point at;
    public int timestep;
  }

  private ResolveResult tryWithResolvingPaths(Plan parentPlan) {
    Plan myPlan = memory.getPlan(this);
    
    FloodGrid distanceGrid = getDistanceGrid();
    
    System.out.println("distance-grid");
    print(distanceGrid.distances);
    
    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;
      
      List<Plan> failedPlans = memory.getFailedPlans(this);
      List<Point> resolvedPath = getDeadlockResolvingPath(failedPlans, distanceGrid);
      if (resolvedPath == null) return ResolveResult.failed;
      
      myPlan.addToPlan(resolvedPath);
      
      List<Agent> conflictingAgents = getConflictingAgents();
      
      boolean ok = true;
      for (Agent agent : conflictingAgents) {
        Plan childPlan = memory.getPlan(agent);
        myPlan.addDependency(childPlan);
        
        ResolveResult result = agent.resolve(myPlan, Phase.resolving);
        if (result == ResolveResult.failed) {
          // only undo the child plans which found resolving route.
          // the child plans failing do not have a resolving route to undo below.
          myPlan.removeDependency(childPlan); 
          
          ok = false;
          
          break;
        }
      }
      
      if (ok) {
        return ResolveResult.ok;
        
      } else {  // undo and try another resolving path
        memory.addFailedPlan(myPlan);
        
        myPlan.undoDependencyLevel();
        myPlan.undo();
      }
    }
    
    return ResolveResult.failed;
  }


  private ResolveResult tryWithOutposts(Plan parentPlan, Phase phase) {
    // If the parentPlan is the requesters plan, then we can try to resolve the deadlock by stalling the requesters plan.
    // If the parentPlan is not the requesters plan, then this plan fails, and the parentPlan has to find another way to resolve its deadlock.
    // an agent can outpost two successive times. If this happens, the the first outpost should select another outpost in the next iteration.
    if (phase == Phase.outposting) return ResolveResult.failed;

    Plan myPlan = memory.getPlan(this);
    Plan requestPlan = memory.request;
    Agent requestAgent = memory.request.agent;
    
    List<Point> originalRequestPlanPath = new ArrayList<>();
    originalRequestPlanPath.add(requestPlan.agent.getPosition());
    originalRequestPlanPath.addAll(requestPlan.path);
    
    List<Point> oldRequestPlanPath = requestPlan.undo();
    FloodGrid outpostDistanceGrid = getOutpostDistanceGrid();
    
    System.out.println("outpost-grid");
    print(outpostDistanceGrid.distances);
    
    List<Point> outposts = getOutposts(outpostDistanceGrid.distances);
    //outposts.remove(0); // @hack @cleanup
    
    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;
      
      List<Plan> failedPlans = memory.getFailedPlans(this);
      List<Point> outpostPath = getOutpostPath(outposts, failedPlans, outpostDistanceGrid);
      if (outpostPath == null) break;
      
      myPlan.addToPlan(outpostPath);

      List<Agent> conflictingAgents = getConflictingAgents();

      boolean ok = true;
      for (Agent agent : conflictingAgents) {
        Plan childPlan = memory.getPlan(agent);
        myPlan.addDependency(childPlan);
        
        ResolveResult result = agent.resolve(myPlan, Phase.resolving);
        if (result == ResolveResult.failed) {
          // only undo the child plans which found resolving route.
          // the child plans failing do not have a resolving route to undo below.
          myPlan.removeDependency(childPlan); 
          
          ok = false;
          break;
        }
      }
      
      if (ok) {
        
        Point requesterTarget = oldRequestPlanPath.get(oldRequestPlanPath.size() - 1);
        
        List<Point> path = findPath(requestAgent, requesterTarget, originalRequestPlanPath);
        
        if (path == null) {
          // undo/fail
          Assert.that(path != null, "todo!");

          return ResolveResult.failed;
        } else {
          
          requestPlan.addToPlan(path);
          requestPlan.pushDependencyLevel();
          
          conflictingAgents = requestAgent.getConflictingAgents();
          Assert.that(conflictingAgents.contains(this));
          
          ok = true;
          for (Agent agent : conflictingAgents) {
            Plan childPlan = memory.getPlan(agent);
            requestPlan.addDependency(childPlan);
            
            Phase agentPhase = equals(agent) ? Phase.outposting : Phase.resolving;
            ResolveResult result = agent.resolve(requestPlan, agentPhase);
            if (result == ResolveResult.failed) {
              // only undo the child plans which found resolving route.
              // the child plans failing do not have a resolving route to undo below.
              requestPlan.removeDependency(childPlan); 
              
              ok = false;
              break;
            }
          }
          
          if (ok) {
            return ResolveResult.ok;
            
          } else { // try another outpost.
            memory.addFailedPlan(requestPlan);
            
            requestPlan.undo();
            requestPlan.undoDependencyLevel();
            requestPlan.popDependencyLevel();
          }
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

    requestPlan.addToPlan(oldRequestPlanPath);
    
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

  private List<Point> findPath(Agent agent, Point target, List<Point> tiles) {
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    moves.add(new Point(0, 0));

    int[][] grid = createGrid(tiles);
    fill(grid, -1); // walls
    
    for (Point tile : tiles) {
      grid[tile.x][tile.y] = 0;
    }
    
    int[][] occupied = createGrid(tiles);
    int[][] prevOccupied = createGrid(tiles);
    
    List<Plan> plans = memory.plans;
    
    AStarPathFinder pathfinder = new AStarPathFinder() {
      @Override
      public List<Point> getMoves(Point at, int timestep) {
        agent.updateOccupiedTiles(prevOccupied, timestep, plans, memory.agents);
        agent.updateOccupiedTiles(occupied, timestep + 1, plans, memory.agents);
        
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

    Point from = agent.getPosition();
    int timestamp = agent.path.size();
    
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
        if (!board.isTileOpen(x, y)) continue;  // wall or outside board
        if (!isOutpost(x, y, outpostDistanceGrid)) continue;
        
        Point endpoint = new Point(x, y);
        outposts.add(endpoint);
      }
    }
    
    return outposts;
  }
  
  private FloodGrid getOutpostDistanceGrid() {
    return getDistanceGrid();
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

        Point otherLastAt = plan.getPosition();
        
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
  private List<Point> getOutpostPath(List<Point> outposts, List<Plan> failedPlans, FloodGrid grid) {
    while (outposts.size() > 0) {
      Point endPoint = outposts.remove(0);
      
      List<Point> path = traverse(endPoint, grid);
      if (path == null) continue; // if no path exists for the endpoint try with another endpoint
      
      boolean alreadyFailed = false;
      for (Plan failed : failedPlans) {
        Point last = failed.getPosition();
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
  
  private List<Point> getDeadlockResolvingPath(List<Plan> failedPlans, FloodGrid grid) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] havenGrid = new int[width][height]; // ok: 1, wall: 0, agent: -1, failed plans: -2
    copy(board.grid, havenGrid);
    
    List<Plan> plans = memory.plans;
    for (Plan plan : plans) {
      if (plan.agent.equals(memory.request.agent)) continue;

      for (int i = 0; i <= plan.path.size() - 2; i++) { // the last position is skipped, because the agent can push the other agent.
        Point point = plan.path.get(i);
        havenGrid[point.x][point.y] = -1;
      }
    }
    
    for (int i = 0; i <= memory.request.path.size() - 1; i++) { // we can't push the "root"/requester
      Point point = memory.request.path.get(i);
      havenGrid[point.x][point.y] = -1;
    }
    
    // @TODO: outdated :failedplans:
    for (Plan plan : failedPlans) {
      Point last = plan.getPosition();
      havenGrid[last.x][last.y] = -2;
    }
    
    List<Point> endPoints = new ArrayList<>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (havenGrid[x][y] == 0) continue;  // wall
        if (havenGrid[x][y] == -1) continue;  // agent
        if (havenGrid[x][y] == -2) continue;  // failed plans @TODO: :failedplans:
        
        if (grid.distances[x][y] == -1) continue; // no path leads to the endpoint.
        if (grid.distances[x][y] == 0) continue; // if agent1 moves to agent2 (this agent) and stops, then agent2 can't push agent1 again. 
        
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
      
      List<Point> longPath = traverse(endPoint, grid);
      if (longPath == null) continue; // if no path exists for the endpoint try with another endpoint
      
      longPath.remove(0); // remove the first point, because the agent is already there.
      List<Point> path = shortenPath(longPath, havenGrid);
      
      Point shortestEndpoint = path.get(path.size() - 1);
      if (havenGrid[shortestEndpoint.x][shortestEndpoint.y] != -2) return path; // if this path is _not_ one of the failed paths, then use it as the path, else try another endpoint.
    }
    
    return null;
  }

  private List<Point> shortenPath(List<Point> longPath, int[][] havenGrid) {
    List<Point> path = new ArrayList<>();
    
    for (Point point : longPath) {
      path.add(point);
      
      if (havenGrid[point.x][point.y] == 1) break;  // the first safe spot has been found. Lets stop.
    }
    
    return path;
  }

  private List<Point> traverse(Point endPoint, FloodGrid grid) {
    List<Point> path = new ArrayList<>();
    path.add(endPoint);
    
    Point current = endPoint;
    
    while (grid.distances[current.x][current.y] != 0) {
      int cost = grid.distances[current.x][current.y];
      
      Point backtrack = grid.backtracks[current.x][current.y];
      int nextCost = grid.distances[backtrack.x][backtrack.y];
      
      int repeats = cost - nextCost;
      for (int i = 0; i < repeats; i++) {
        path.add(backtrack.copy());
      }
      
      current = backtrack;
    }
    
    Collections.reverse(path);
    
    return path;
  }


  private FloodGrid getDistanceGrid() {
    Board board = memory.board;
    
    List<Plan> plans = memory.plans;
    List<Agent> agents = memory.agents;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] distanceGrid = new int[width][height]; // no path found (yet): -1, distance: x (>= 0)
    Point[][] backtrackGrid = new Point[width][height]; // null: no path

    int[][] occupied = new int[width][height]; // free: -1, id: x (>= 0)
    int[][] nextOccupied = new int[width][height]; // free: -1, id: x (>= 0)
    
    fill(nextOccupied, -1);
    fill(distanceGrid, -1);
    
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    Plan plan = memory.getPlan(this);
    int timestep = path.size() + plan.path.size();

    Point at = plan.getPosition();
    if (at == null) at = path.get(path.size() - 1);
    
    List<Point> pending = new ArrayList<>();
    pending.add(at);
    
    updateOccupiedTiles(occupied, timestep, plans, agents);
    updateOccupiedTiles(nextOccupied, timestep + 1, plans, agents);
    
    //print(occupied);

    int pendingInLayerCount = pending.size();
    
    distanceGrid[at.x][at.y] = 0;
    int minDistance = 1;
    
    int maxIterations = 1000;
    int iteration = 0;
    
    // flood-fill algorithm
    while (pending.size() > 0) {
      pendingInLayerCount -= 1;

      iteration += 1;
      if (iteration >= maxIterations) break;
      
      Point current = pending.remove(0);

      boolean anyNeighboursNotEncountered = false;
      
      for (Point move : moves) {
        int tx = current.x + move.x;
        int ty = current.y + move.y;
        
        if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
        if (distanceGrid[tx][ty] != -1) continue;  // already visited
        
        if (nextOccupied[tx][ty] != -1) { // currently occupied by another agent but not visited
          anyNeighboursNotEncountered = true;

        } else {
          if (occupied[tx][ty] != -1 && occupied[tx][ty] == nextOccupied[current.x][current.y]) continue;  // the agents would jump through each other. Not possible.
          
          distanceGrid[tx][ty] = minDistance;
          backtrackGrid[tx][ty] = current;
          
          Point child = new Point(tx, ty);
          pending.add(child);
        }
      }
      
      // an agent may need to stall for some time before a tile becomes available.
      if (nextOccupied[current.x][current.y] == -1 && anyNeighboursNotEncountered) {
        pending.add(current);
      }

      if (pendingInLayerCount == 0) {
        minDistance += 1;
        timestep += 1;
        
        pendingInLayerCount = pending.size();
        
        updateOccupiedTiles(occupied, timestep, plans, agents);
        updateOccupiedTiles(nextOccupied, timestep + 1, plans, agents);
        
        //print(occupied);
      }
    }
    
    FloodGrid grid = new FloodGrid();
    grid.distances = distanceGrid;
    grid.backtracks = backtrackGrid;
    
    return grid;
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


class Plan {
  public Agent agent;

  public List<Point> path;
  public List<Integer> checkpoints;
  
  public List<DependencyLevel> dependencyLevels;
  
  public Plan() {
    path = new ArrayList<>();
    checkpoints = new ArrayList<>();
    dependencyLevels = new ArrayList<>();
    
    pushDependencyLevel();
  }

  public void removeDependency(Plan plan) {
    DependencyLevel level = dependencyLevels.get(dependencyLevels.size() - 1);
    level.dependencies.remove(plan);
  }

  public void addToPlan(List<Point> addition) {
    int checkpoint = path.size();
    
    checkpoints.add(checkpoint);
    path.addAll(addition);
  }
  
  public Point getPosition() {
    if (path.size() == 0) return null;
    return path.get(path.size() - 1);
  }
  
  public void pushDependencyLevel() {
    DependencyLevel level = new DependencyLevel();
    level.dependencies = new ArrayList<>();
    dependencyLevels.add(level);
  }
  
  public void undoDependencyLevel() {
    DependencyLevel level = dependencyLevels.get(dependencyLevels.size() - 1);
    
    for (Plan plan : level.dependencies) {
      plan.undo();
    }
  }
  
  public void popDependencyLevel() {
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

class FloodGrid {
  public int[][] distances;
  public Point[][] backtracks;
}
