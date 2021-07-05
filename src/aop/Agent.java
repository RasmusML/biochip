package aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dmb.algorithms.Point;
import dmb.helpers.Assert;
import framework.math.MathUtils;

public class Agent {

  public Plan plan;

  private int id;
  private SharedAgentMemory memory;
  
  private List<Point> path;

  public Agent(SharedAgentMemory memory, int id, Point... spawn) {
    this.id = id;
    this.memory = memory;

    plan = new Plan();
    plan.agent = this;
    
    path = new ArrayList<>();
    
    for (Point point : spawn) {
      path.add(point);
    }
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

    DependencyLevel rootLevel = plan.pushRootDependencyLevel();

    List<Agent> pushableAgents = getPushableAgents();

    boolean ok = true;
    for (Agent agent : pushableAgents) {
      ResolveResult result = agent.resolve(plan, rootLevel);

      if (result == ResolveResult.failed) {
        ok = false;
        break;
      }
    }

    Assert.that(plan.checkpoints.size() == 1);

    if (ok) {
      for (Agent agent : memory.agents) {
        agent.path.addAll(agent.plan.path);
        agent.plan.reset();
      }
    }

    /*
     * It is not possible to undo and pop the root level, because the requester may have multiple DependencyLevels, due to outpost re-pathing.
     * However, there will always only be a single checkpoint! Because if re-path occurs, then the previous check-point is removed (through the path undo)
     * 
     * rootLevel.undo();
     * plan.popDependencyLevel(rootLevel);
     * 
     * We can still clear the levels, but the DependencyLevels will not be unrolled (not that it matter).
     */
    plan.dependencyLevels.clear();

    memory.request = null;
    memory.failedPlans.clear();
    memory.agents.clear();

    return ok ? ResolveResult.ok : ResolveResult.failed;
  }

  public int getId() {
    return id;
  }

  public ResolveResult resolve(Plan parentPlan, DependencyLevel parentLevel) {
    Assert.that(!plan.equals(memory.request));

    if (isResolved(parentPlan)) return ResolveResult.ok;

    if (isCircularDependency(parentLevel)) return ResolveResult.failed;

    DependencyLevel myLevel = plan.pushDependencyLevel(parentLevel);
    ResolveResult result;

    result = tryWithSideStepping(parentPlan, myLevel);
    if (result == ResolveResult.ok) return ResolveResult.ok;

    // try finding a haven cell.
    result = tryWithResolvingPaths(parentPlan, myLevel);
    if (result == ResolveResult.ok) return ResolveResult.ok;

    // try pushing the parent back.
    result = tryWithPushingParentBack(parentPlan, myLevel);
    if (result == ResolveResult.ok) return ResolveResult.ok;

    // try stalling the requester.
    result = tryWithOutposts(parentPlan, myLevel);
    if (result == ResolveResult.ok) return ResolveResult.ok;

    plan.popDependencyLevel(myLevel);

    // only undo the child plans which found resolving route.
    // the child plans failing do not have a resolving route to undo below.
    parentLevel.removeDependency(myLevel);

    return ResolveResult.failed;
  }

  private ResolveResult tryWithPushingParentBack(Plan parentPlan, DependencyLevel myLevel) {
    if (parentPlan.equals(memory.request)) return ResolveResult.failed; // it is not possible to push the requester back.

    Point at = plan.getPosition();
    if (at == null) at = plan.agent.getPosition();

    Point parentTarget = parentPlan.getPosition();
    if (!(parentTarget.x == at.x && parentTarget.y == at.y)) return ResolveResult.failed; // a push back move does not make sense, because the parent is just moving through.

    Point pushBack = at.copy();

    int meTime = path.size() + plan.path.size();
    int mePushedAwayTime = parentPlan.agent.path.size() + parentPlan.path.size();
    int meJustBeforePushedAwayTime = mePushedAwayTime - 1;

    List<Point> stays = new ArrayList<>();

    int stayBy = meJustBeforePushedAwayTime - meTime;
    for (int i = 0; i < stayBy; i++) {
      stays.add(at.copy());
    }

    List<Out> outs = getOuts(at, meJustBeforePushedAwayTime);

    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;

      if (outs.size() == 0) break;

      Out out = outs.remove(0);

      List<Point> path = new ArrayList<>();
      path.addAll(stays);

      int stayByPushed = out.timestep - meJustBeforePushedAwayTime;
      for (int i = 0; i < stayByPushed; i++) {
        path.add(out.at);
      }
      
      path.add(pushBack);

      plan.addToPlan(path);

      List<Agent> pushableAgents = getPushableAgents();
      Assert.that(pushableAgents.contains(parentPlan.agent));
      pushableAgents.remove(parentPlan.agent);

      boolean ok = true;
      for (Agent agent : pushableAgents) {
        ResolveResult result = agent.resolve(plan, myLevel);
        if (result == ResolveResult.failed) {
          ok = false;
          break;
        }
      }

      if (ok) {
        ResolveResult result = parentPlan.agent.resolve(plan, myLevel); // if "pushingParentBack" is used, then we can't do a push with 3 agents: ABC, only with 2 agents: AB
        if (result == ResolveResult.ok) {
          return ResolveResult.ok;
        } else {
          memory.addFailedPlan(plan);

          myLevel.undo();
          plan.undo();
        }

      } else {
        memory.addFailedPlan(plan);

        myLevel.undo();
        plan.undo();
      }
    }

    return ResolveResult.failed;
  }
  
  private int getFirstCollisionTime(Agent other) {
    int mySteps = path.size() + plan.path.size();
    int otherSteps = other.path.size() + other.plan.path.size();
    
    int totalTimesteps = Math.max(mySteps, otherSteps);
    int timestep = 1;
    while (timestep <= totalTimesteps) {
      Point at = getPositionSafe(timestep);
      Point otherAt = other.getPositionSafe(timestep);
      if (at.x == otherAt.x && at.y == otherAt.y) return timestep;
      timestep += 1;
    }
    
    return -1;
  }
  
  private Point getPositionSafe(int timestep) {
    Point at;
    
    int index;
    
    index = timestep - 1;
    at = getPosition(index);
    if (at != null) return at;
    
    if (plan.path.size() == 0) return getPosition();
    
    index = timestep - path.size() - 1;
    if (index >= plan.path.size()) return plan.getPosition();
    
    return plan.path.get(index);
  }

  private ResolveResult tryWithSideStepping(Plan parentPlan, DependencyLevel myLevel) {
    if (parentPlan.equals(memory.request)) return ResolveResult.failed;

    Point at = plan.getPosition();
    if (at == null) at = plan.agent.getPosition();

    List<Point> stays = new ArrayList<>();

    int collisionTime = parentPlan.agent.getFirstCollisionTime(this);
    
    int meTime = path.size() + plan.path.size();
    int stayBy = collisionTime - meTime - 1;
    for (int i = 0; i < stayBy; i++) {
      stays.add(at.copy());
    }
    
    int meJustBeforePushedAwayTime = collisionTime - 1;

    List<Out> outs = getOuts(at, meJustBeforePushedAwayTime);

    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;

      if (outs.size() == 0) break;

      Out out = outs.remove(0);
      if (out.collision) continue;

      List<Point> path = new ArrayList<>();
      path.addAll(stays);

      path.add(out.at);
      
      plan.addToPlan(path);

      List<Agent> pushableAgents = getPushableAgents();

      boolean ok = true;
      for (Agent agent : pushableAgents) {
        ResolveResult result = agent.resolve(plan, myLevel);
        if (result == ResolveResult.failed) {
          ok = false;
          break;
        }
      }

      if (ok) {
        return ResolveResult.ok;
      } else {
        memory.addFailedPlan(plan);

        myLevel.undo();
        plan.undo();
      }
    }

    return ResolveResult.failed;
  }

  private List<Out> getOuts(Point baseAt, int timestep) {
    Board board = memory.board;

    List<Agent> agents = memory.agents;

    int width = board.getWidth();
    int height = board.getHeight();

    int[][] occupied = new int[width][height]; // free: -1, id: x (>= 0)
    int[][] nextOccupied = new int[width][height]; // free: -1, id: x (>= 0)

    updateOccupiedTiles(occupied, timestep, agents);
    updateOccupiedTiles(nextOccupied, timestep + 1, agents);

    /*
    print(occupied);
    print(nextOccupied);
    */

    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));

    List<Out> outs = new ArrayList<>();
    for (Point move : moves) {
      Point to = baseAt.copy().add(move);

      if (!board.isTileOpen(to.x, to.y)) continue;

      if (nextOccupied[to.x][to.y] != -1) continue;
      if (occupied[to.x][to.y] != -1 && occupied[to.x][to.y] == nextOccupied[baseAt.x][baseAt.y]) continue; // the agents would jump through each other. Not possible.

      Out out = new Out();
      out.at = to;
      out.timestep = timestep + 1;

      outs.add(out);
    }

    List<Out> pending = new ArrayList<>();
    pending.addAll(outs);

    int maxIterations = 100;
    int requesterLength = memory.request.agent.path.size() + memory.request.path.size();
    int iterationsNeeded = requesterLength - timestep - 1;

    int iterations = Math.min(iterationsNeeded, maxIterations);

    //timestep += 1;

    updateOccupiedTiles(occupied, timestep, agents);
    updateOccupiedTiles(nextOccupied, timestep + 1, agents);

    for (int i = 0; i < iterations; i++) {

      for (Iterator<Out> it = pending.iterator(); it.hasNext();) {
        Out out = it.next();
        Point at = out.at;

        // currently occupied by another agent but not visited
        if (nextOccupied[at.x][at.y] != -1) {
          out.collision = true;
          it.remove();
        
        } else {
          out.timestep = timestep + 1;
        }
      }

      timestep += 1;

      updateOccupiedTiles(occupied, timestep, agents);
      updateOccupiedTiles(nextOccupied, timestep + 1, agents);
    }

    return outs;

  }

  class Out {
    public Point at;
    public int timestep;
    public boolean collision;
  }

  private ResolveResult tryWithResolvingPaths(Plan parentPlan, DependencyLevel myLevel) {
    FloodGrid endpointGrid = getDistanceGrid();

    /*
    System.out.println("distance-grid " + id);
    print(endpointGrid.distances);
    */

    int[][] havenGrid = getEndPointHavenGrid(endpointGrid.distances);
    List<Point> endPoints = getEndpoints(endpointGrid, havenGrid);
    //printEndPoints(endPoints);

    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;

      List<Point> resolvedPath = getDeadlockResolvingPath(endPoints, endpointGrid, havenGrid);
      if (resolvedPath == null) return ResolveResult.failed;

      plan.addToPlan(resolvedPath);

      List<Agent> pushableAgents = getPushableAgents();

      boolean ok = true;
      for (Agent agent : pushableAgents) {
        ResolveResult result = agent.resolve(plan, myLevel);
        if (result == ResolveResult.failed) {
          ok = false;
          break;
        }
      }

      if (ok) {
        return ResolveResult.ok;

      } else { // undo and try another resolving path
        memory.addFailedPlan(plan);

        myLevel.undo();
        plan.undo();
      }
    }

    return ResolveResult.failed;
  }

  private List<Point> getEndpoints(FloodGrid grid, int[][] havenGrid) {
    Board board = memory.board;

    int width = board.getWidth();
    int height = board.getHeight();

    List<Point> endPoints = new ArrayList<>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (havenGrid[x][y] == 0) continue; // wall
        if (havenGrid[x][y] == -1) continue; // agent

        if (grid.distances[x][y] == -1) continue; // no path leads to the endpoint.
        if (grid.distances[x][y] == 0) continue; // if agent1 moves to agent2 (this agent) and stops, then agent2 can't push agent1 again. 

        Point endpoint = new Point(x, y);
        endPoints.add(endpoint);
      }
    }

    //sortCellsByClosestToAgent(endPoints);

    return endPoints;
  }

  private void sortCellsByClosestToAgent(List<Point> endPoints) {
    final Point at = (plan.getPosition() == null) ? getPosition() : plan.getPosition();

    Collections.sort(endPoints, (p1, p2) -> {
      int d1 = (int) MathUtils.getManhattanDistance(at.x, at.y, p1.x, p1.y);
      int d2 = (int) MathUtils.getManhattanDistance(at.x, at.y, p2.x, p2.y);
      return d1 - d2;
    });
  }

  private void printEndPoints(List<Point> endPoints) {
    System.out.println("endpoints:");
    for (Point endpoint : endPoints) {
      System.out.println(">" + endpoint);
    }
    System.out.println();
  }

  private int[][] getEndPointHavenGrid(int[][] distanceGrid) {
    Board board = memory.board;

    int width = board.getWidth();
    int height = board.getHeight();

    int[][] havenGrid = new int[width][height]; // ok: 1, wall: 0, agent: -1
    copy(board.grid, havenGrid);

    for (Agent agent :  memory.agents) {
      Plan otherPlan = agent.plan;
      if (otherPlan.equals(plan)) continue;
      if (otherPlan.agent.equals(memory.request.agent)) continue;
      if (otherPlan.path.size() == 0) continue;

      // the last position is skipped if this agent arrives after the other agent, because the agent can push the other agent.
      for (int i = 0; i <= otherPlan.path.size() - 2; i++) {
        Point point = otherPlan.path.get(i);
        havenGrid[point.x][point.y] = -1;
      }

      Point last = otherPlan.path.get(otherPlan.path.size() - 1);

      int steps = distanceGrid[last.x][last.y];
      int meArrives = path.size() + plan.path.size() + steps;
      int otherArrives = otherPlan.agent.path.size() + otherPlan.path.size();

      if (meArrives <= otherArrives) havenGrid[last.x][last.y] = -1;
    }

    for (int i = 0; i <= memory.request.path.size() - 1; i++) { // we can't push the "root"/requester
      Point point = memory.request.path.get(i);
      havenGrid[point.x][point.y] = -1;
    }

    return havenGrid;
  }

  private ResolveResult tryWithOutposts(Plan parentPlan, DependencyLevel myLevel) {
    Plan requestPlan = memory.request;
    // no undoing left to be done. 
    // This can happen, if multiple agents try to do an outpost and the requester does not get to find a new path in between, 
    // because pushable agents exists when moving to the outpost. 
    if (requestPlan.path.size() == 0) return ResolveResult.failed;

    Agent requestAgent = memory.request.agent;

    List<Point> originalRequestPlanPath = new ArrayList<>();

    // requester can only use the cells on the path for the re-search.
    originalRequestPlanPath.add(requestPlan.agent.getPosition());
    originalRequestPlanPath.addAll(requestPlan.path);

    // @note: requester can use any cell for the re-search during this. However, it may become to expensive, so only use the path already specified by the agent.
    //originalRequestPlanPath.addAll(memory.board.openTiles);

    List<Point> oldRequestPlanPath = requestPlan.undo();
    FloodGrid outpostGrid = getOutpostDistanceGrid();

    /*
    System.out.println("outpost-grid " + id);
    print(outpostGrid.distances);
    */

    List<Point> outposts = getOutposts(outpostGrid.distances);

    List<Point> outpostsLeft = new ArrayList<>();
    outpostsLeft.addAll(outposts);

    while (memory.tryCount < memory.totalTries) {
      memory.tryCount += 1;

      List<Point> outpostPath = getOutpostPath(outpostsLeft, outpostGrid);

      if (outpostPath == null) break; // try another outposts (if any left).

      plan.addToPlan(outpostPath);

      List<Agent> pushableAgents = getPushableAgents();

      boolean ok = true;
      for (Agent agent : pushableAgents) {
        ResolveResult result = agent.resolve(plan, myLevel);
        if (result == ResolveResult.failed) {
          ok = false;
          break;
        }
      }

      if (ok) {
        Point requesterTarget = oldRequestPlanPath.get(oldRequestPlanPath.size() - 1);

        List<Point> path = findPath(requestAgent, requesterTarget, originalRequestPlanPath);

        if (path == null) {
          memory.addFailedPlan(plan);

          myLevel.undo();
          plan.undo();

        } else {
          requestPlan.addToPlan(path);
          DependencyLevel requesterLevel = requestPlan.pushRootDependencyLevel();

          pushableAgents = requestAgent.getPushableAgents();

          ok = true;
          for (Agent agent : pushableAgents) {
            ResolveResult result = agent.resolve(requestPlan, requesterLevel);
            if (result == ResolveResult.failed) {
              ok = false;
              break;
            }
          }

          if (ok) {
            return ResolveResult.ok;

          } else { // try another outpost.
            memory.addFailedPlan(requestPlan);

            requesterLevel.undo();
            requestPlan.undo();

            myLevel.undo();
            plan.undo();
          }
        }

      } else {
        memory.addFailedPlan(plan);

        myLevel.undo();
        plan.undo();
      }
    }

    requestPlan.addToPlan(oldRequestPlanPath); // redo the undo of the requester plan.

    return ResolveResult.failed;
  }

  private boolean isCircularDependency(DependencyLevel parentLevel) {
    if (plan.dependencyLevels.size() == 0) return false;

    int occurrence = 1;
    int maxOccurences = 2;
    
    DependencyLevel ancestor = parentLevel;

    while (ancestor != null) {
      if (plan.equals(ancestor.myPlan)) occurrence += 1;
      ancestor = ancestor.parent;
    }

    return occurrence > maxOccurences;
  }

  private void printCircularDependency(DependencyLevel myLevel) {
    DependencyLevel ancestor = myLevel.parent;

    System.out.printf("%d<-", plan.agent.id);

    while (ancestor != null) {
      System.out.printf("%d<-", ancestor.myPlan.agent.id);
      ancestor = ancestor.parent;
    }

    System.out.println();

  }

  private boolean isResolved(Plan parentPlan) {
    Agent agent = parentPlan.agent;
    List<Agent> pushableAgents = agent.getPushableAgents();
    if (pushableAgents.contains(this)) return false;
    return true;
  }

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

    int[][] nextOccupied = createGrid(tiles);
    int[][] occupied = createGrid(tiles);

    AStarPathFinder pathfinder = new AStarPathFinder() {
      @Override
      public List<Point> getMoves(Point at, int timestep) {
        agent.updateOccupiedTiles(occupied, timestep, memory.agents);
        agent.updateOccupiedTiles(nextOccupied, timestep + 1, memory.agents);

        List<Point> validMoves = new ArrayList<>();
        for (Point move : moves) {
          Point to = at.copy().add(move.x, move.y);
          if (to.x < 0 || to.x >= nextOccupied.length || to.y < 0 || to.y >= nextOccupied[0].length) continue;
          if (grid[at.x][at.y] == -1) continue; // wall

          if (nextOccupied[to.x][to.y] != -1) continue;
          if (occupied[to.x][to.y] != -1 && occupied[to.x][to.y] == nextOccupied[at.x][at.y]) continue; // the agents would jump through each other. Not possible.

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
        if (!board.isTileOpen(x, y)) continue; // wall or outside board
        if (!isOutpost(x, y, outpostDistanceGrid)) continue;

        if (outpostDistanceGrid[x][y] == 0) continue; // don't use the current position as an outpost.

        Point endpoint = new Point(x, y);
        outposts.add(endpoint);
      }
    }

    //sortCellsByClosestToAgent(outposts);

    // this makes sense when there are a lot of outposts e.g. on an assay
    int maxOutposts = 5;
    int numberOfOutposts = Math.min(maxOutposts, outposts.size());
    outposts = outposts.subList(0, numberOfOutposts);

    return outposts;
  }

  private FloodGrid getOutpostDistanceGrid() {
    return getDistanceGrid();
  }

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

      if (!board.isTileOpen(tx, ty)) continue; // wall or outside the board
      if (overlay[tx][ty] == -1) continue;
      openNeighbourTiles += 1;
    }

    return openNeighbourTiles >= 3;
  }

  public List<Agent> getPushableAgents() {
    List<Agent> pushableAgents = new ArrayList<>();

    List<Agent> agents = new ArrayList<>();
    agents.addAll(memory.agents);
    agents.remove(memory.request.agent);
    agents.remove(this);

    for (int i = 0; i < plan.path.size(); i++) {
      Point at = plan.path.get(i);

      for (Iterator<Agent> it = agents.iterator(); it.hasNext();) {
        Agent other = it.next();

        Plan otherPlan = other.plan;

        if (otherPlan.path.size() == 0) {
          Point otherLastAt = other.getPosition();
          if (at.x == otherLastAt.x && at.y == otherLastAt.y) {
            pushableAgents.add(other);
            it.remove();
          }

        } else {
          int offsetTimestep = i + 1;
          int endTime = plan.agent.path.size() + offsetTimestep;

          int otherEndTime = other.getPath().size() + otherPlan.path.size();
          if (otherEndTime < endTime) {

            Point otherLastAt = otherPlan.getPosition();
            if (at.x == otherLastAt.x && at.y == otherLastAt.y) {
              pushableAgents.add(other);
              it.remove();
            }
          }
        }

      }
    }

    return pushableAgents;
  }

  // outposts are consumed here.
  private List<Point> getOutpostPath(List<Point> outposts, FloodGrid grid) {
    List<Plan> failedPlans = memory.getFailedPlans(this);

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

  private List<Point> getDeadlockResolvingPath(List<Point> endPoints, FloodGrid grid, int[][] havenGrid) {
    while (endPoints.size() > 0) {
      Point endPoint = endPoints.remove(0);

      List<Point> longPath = traverse(endPoint, grid);
      if (longPath == null) continue; // if no path exists for the endpoint try with another endpoint

      longPath.remove(0); // remove the first point, because the agent is already there.
      List<Point> path = shortenPath(longPath, havenGrid);

      if (!isFailedPlan(path)) return path;
    }

    return null;
  }

  // if the full path is same length as a failed plan, the endpoints are the same and the failed plan is the same agent, then it is a match.
  private boolean isFailedPlan(List<Point> path) {
    List<Plan> failedPlans = memory.getFailedPlans(this);

    List<Point> fullPath = new ArrayList<>();
    fullPath.addAll(plan.path);
    fullPath.addAll(path);

    Point end = fullPath.get(fullPath.size() - 1);

    for (Plan failed : failedPlans) {
      if (!failed.agent.equals(this)) continue;
      if (fullPath.size() != failed.path.size()) continue;

      Point failedEnd = failed.path.get(failed.path.size() - 1);
      if (end.x == failedEnd.x && end.y == failedEnd.y) {
        return true;
      }
    }

    return false;
  }

  private List<Point> shortenPath(List<Point> longPath, int[][] havenGrid) {
    List<Point> path = new ArrayList<>();

    for (Point point : longPath) {
      path.add(point);

      if (havenGrid[point.x][point.y] == 1) break; // the first safe spot has been found. Lets stop.
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

    List<Agent> agents = memory.agents;

    int width = board.getWidth();
    int height = board.getHeight();

    int[][] distanceGrid = new int[width][height]; // no path found (yet): -1, distance: x (>= 0)
    Point[][] backtrackGrid = new Point[width][height]; // null: no path

    int[][] occupied = new int[width][height]; // free: -1, id: x (>= 0)
    int[][] nextOccupied = new int[width][height]; // free: -1, id: x (>= 0)

    //fill(nextOccupied, -1);
    fill(distanceGrid, -1);

    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));

    int timestep = path.size() + plan.path.size();

    Point at = plan.getPosition();
    if (at == null) at = path.get(path.size() - 1);

    List<Point> pending = new ArrayList<>();
    pending.add(at);

    updateOccupiedTiles(occupied, timestep, agents);
    updateOccupiedTiles(nextOccupied, timestep + 1, agents);

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

        if (!board.isTileOpen(tx, ty)) continue; // wall or outside the board
        if (distanceGrid[tx][ty] != -1) continue; // already visited

        if (nextOccupied[tx][ty] != -1) { // currently occupied by another agent but not visited
          anyNeighboursNotEncountered = true;

        } else {
          if (occupied[tx][ty] != -1 && occupied[tx][ty] == nextOccupied[current.x][current.y]) continue; // the agents would jump through each other. Not possible.

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

        updateOccupiedTiles(occupied, timestep, agents);
        updateOccupiedTiles(nextOccupied, timestep + 1, agents);
      }
    }

    FloodGrid grid = new FloodGrid();
    grid.distances = distanceGrid;
    grid.backtracks = backtrackGrid;

    return grid;
  }

  // free: -1, id: x (>= 0)
  private void updateOccupiedTiles(int[][] occupied, int timestep, List<Agent> agents) {
    fill(occupied, -1);

    for (Agent agent : agents) {
      if (equals(agent)) continue;

      Plan plan = agent.plan;
      
      int stepsCommitted = agent.path.size();
      int stepsPlanned = plan.path.size();
      int steps = stepsCommitted + stepsPlanned;

      if (stepsCommitted >= timestep) continue;
      if (timestep > steps) continue;

      int step = timestep - stepsCommitted;
      int stepIndex = step - 1;
      Point at = plan.path.get(stepIndex); // current plan move

      int id = agent.getId();
      occupied[at.x][at.y] = id;
    }

    for (Agent agent : agents) {
      if (equals(agent)) continue;

      int stepsCommitted = agent.path.size();
      if (timestep > stepsCommitted) continue;

      int index = timestep - 1;
      Point at = agent.path.get(index); // current committed move

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
  }
  
  public void reset() {
    path.clear();
    checkpoints.clear();
    dependencyLevels.clear();
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

  public DependencyLevel pushRootDependencyLevel() {
    DependencyLevel level = new DependencyLevel();
    level.myPlan = this;
    level.parent = null;
    level.dependencies = new ArrayList<>();

    dependencyLevels.add(level);

    return level;
  }

  public DependencyLevel pushDependencyLevel(DependencyLevel parent) {
    Assert.that(parent != null);

    DependencyLevel level = new DependencyLevel();
    level.myPlan = this;
    level.parent = parent;
    level.dependencies = new ArrayList<>();

    parent.dependencies.add(level);

    dependencyLevels.add(level);

    return level;
  }

  public void popDependencyLevel(DependencyLevel level) {
    DependencyLevel copy = dependencyLevels.remove(dependencyLevels.size() - 1);
    Assert.that(level.equals(copy));
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
    return copy;
  }
}

class DependencyLevel {
  public Plan myPlan;

  public DependencyLevel parent;

  public List<DependencyLevel> dependencies;

  public void addDependency(DependencyLevel level) {
    dependencies.add(level);
  }

  public void removeDependency(DependencyLevel level) {
    Assert.that(dependencies.contains(level));
    dependencies.remove(level);
  }

  public void undo() {
    // Need to do it in the reverse order; most recently added is processed first,
    // because we "pop" the dependencies from a stack.
    List<DependencyLevel> reversed = new ArrayList<>(dependencies);
    Collections.reverse(reversed);

    for (DependencyLevel dependency : reversed) {
      dependency.undo();

      Plan plan = dependency.myPlan;
      plan.undo();
      plan.popDependencyLevel(dependency);
    }

    // It is necessary to clear the dependencies, because the dependency-level can be re-used if there are multiple outposts or endspoints.
    dependencies.clear();
  }
}

class SharedAgentMemory {

  public int numNodesToExploreInSearch;

  public int totalTries;
  public int tryCount; // share the tryCount between the agents so tries doesn't explode when agents request recursively.

  public List<Agent> agents;

  public Plan request;

  public Board board;

  public List<Plan> failedPlans;

  public SharedAgentMemory(Board board) {
    this.board = board;

    totalTries = 100;
    numNodesToExploreInSearch = 1000;

    agents = new ArrayList<>();

    failedPlans = new ArrayList<>();
  }

  public void addFailedPlan(Plan plan) {
    Assert.that(plan.path.size() > 0);

    failedPlans.add(plan.copy());
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

class FloodGrid {
  public int[][] distances;
  public Point[][] backtracks;
}
