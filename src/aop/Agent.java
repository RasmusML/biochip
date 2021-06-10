package aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dmb.algorithms.Point;
import dmb.helpers.Assert;

public class Agent {

  private int id;

  private SharedAgentMemory memory;
  
  private List<RequestPackage> sendRequests;
  private List<RequestPackage> receivedRequests;
  
  private List<Point> path;
  
  public Agent(SharedAgentMemory memory, int id, Point spawn) {
    this.id = id;
    this.memory = memory;
    
    sendRequests = new ArrayList<>();
    receivedRequests = new ArrayList<>();
    
    path = new ArrayList<>();
    path.add(spawn.copy());
  }
  
  public void addPlan(Plan plan) {
    memory.plans.add(plan);
  }
  
  public Point getPosition(int timestep) {
    if (timestep < 0 || timestep >= path.size()) return null;
    return path.get(timestep);
  }
  
  public void request(RequestPackage pack) {
    sendRequests.add(pack);

    ResolveResult result = pack.receiver.resolve(pack);
    memory.tryCount = 0;
    
    if (pack.request == Request.resolveDeadlock) {
      Plan plan = memory.getPlan(this);

      if (result == ResolveResult.ok) {
        path.addAll(plan.path);
      } else {
        memory.plans.remove(plan);
        memory.failedPlans.add(plan);
      }
    } else {
      throw new IllegalStateException("Request not supported! " + pack.request);
    }
  }
  
  public int getId() {
    return id;
  }

  // tries to commit to a rule which satisfies the request. Rules are explicit contained here.
  public ResolveResult resolve(RequestPackage pack) {
    receivedRequests.add(pack);
    
    if (pack.request == Request.resolveDeadlock) {
      
      /*
      { // part 1 // @incomplete
        
        int[][] senderMap = getCommittedResolvingMap(pack.sender);

        print(senderMap);
        System.out.printf("\n");
        
        List<Plan> failedPlans = memory.getFailedPlans(this);
        List<Point> senderResolvedPath = getResolvingPath(failedPlans, senderMap);
        
        // if the sender makes it impossible for this agent to resolve the deadlock, then stall the sender.
        if (senderResolvedPath == null) {
          return ResolveResult.failed;  
        }
      }
      */
      
      { // part 2
        
        int[][] distanceGrid = getDistanceGrid();
        
        print(distanceGrid);
  
        while (memory.tryCount < memory.totalTries) {
          memory.tryCount += 1;
          
          List<Plan> failedPlans = memory.getFailedPlans(this);
          List<Point> resolvedPath = getDeadlockResolvingPath(failedPlans, distanceGrid);
    
          if (resolvedPath == null) return ResolveResult.failed;  // fail if no possible paths are left to try.
          
          List<Agent> conflictingAgents = getConflictingAgents(resolvedPath);
          Assert.that(conflictingAgents.size() == 0, "not ready to test conflicting agents yet.");
          
          Plan plan = new Plan();
          plan.agent = this;
          plan.path = resolvedPath;
          memory.plans.add(plan);
          
          boolean ok = true;
          for (Agent agent : conflictingAgents) {
            RequestPackage newPack = new RequestPackage();
            newPack.sender = this;
            newPack.receiver = agent;
            newPack.request = Request.resolveDeadlock;
            
            ResolveResult result = agent.resolve(newPack);
            if (result == ResolveResult.failed) {
              ok = false;
              break;
            }
          }
          
          memory.plans.remove(plan);
  
          if (ok) { // commit
            memory.failedPlans.removeAll(failedPlans);
            path.addAll(plan.path);
          
            return ResolveResult.ok;
          } else {
            memory.failedPlans.add(plan);
          }
         }
        
        return ResolveResult.failed;
      }
      
    } else {
      throw new IllegalStateException("unknown request!");
    }
  }
  
  // @TODO: handle circular dependencies.

  private List<Agent> getConflictingAgents(List<Point> path) {
    List<Agent> conflicting = new ArrayList<>();
    
    // @TODO
    
    return conflicting;
  }

  private List<Point> getDeadlockResolvingPath(List<Plan> failedPlans, int[][] distanceGrid) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] overlay = new int[width][height]; // no path found (yet): -1, failedEndPoint: -2, distance: x (>= 0)
    copy(distanceGrid, overlay);
    
    // @TODO: some agents may already have committed there plans, so we should clear those tiles as well (-1)?
    
    for (Plan plan : memory.plans) {
      for (Point point : plan.path) {
        overlay[point.x][point.y] = -1;
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
      
      if (overlay[point.x][point.y] >= 0) break;  // the first safe spot has been found. Lets stop.
    }
    
    return path;
  }

  private List<Point> traverse(Point from, int[][] distanceGrid, int[][] overlay) {
    List<Point> longPath = new ArrayList<>();
    longPath.add(from);
    
    Point current = from;
    while (distanceGrid[current.x][current.y] > 0) {
      
      Point backtrackMove = backtrack(current, distanceGrid, overlay);
      if (backtrackMove == null) return null;
      
      Point to = current.copy().add(backtrackMove.x, backtrackMove.y);
      longPath.add(to);
      current = to;
    }
    
    Collections.reverse(longPath);
    
    return longPath;
  }


  private Point backtrack(Point current, int[][] distanceGrid, int[][] overlay) {
    Board board = memory.board;
    
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    int cost = distanceGrid[current.x][current.y];

    for (Point move : moves) {
      int tx = current.x + move.x;
      int ty = current.y + move.y;
      
      if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
      
      int newCost = distanceGrid[tx][ty];
      if (newCost == cost - 1) return move;
    }
    
    return null;
  }
  
  private int[][] getDistanceGrid() {
    Assert.that(path.size() - 1 == memory.timestep);

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
    
    Point at = path.get(memory.timestep);
    distanceGrid[at.x][at.y] = 0;

    List<Point> pending = new ArrayList<>();
    pending.add(at);
    
    int timestep = memory.timestep;
    updateOccupiedTiles(occupied, prevOccupied, timestep);
    
    // flood-fill algorithm
    while (pending.size() > 0) {
      Point current = pending.remove(0);
      
      int time = distanceGrid[current.x][current.y];
      if (time > timestep) {
        timestep = time;
        updateOccupiedTiles(occupied, prevOccupied, timestep);
      }
      
      for (Point move : moves) {
        int tx = current.x + move.x;
        int ty = current.y + move.y;
        
        if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
        if (distanceGrid[tx][ty] >= 0) continue;  // already visited
        
        if (occupied[tx][ty] != -1) continue; // already occupied
        if (prevOccupied[tx][ty] != -1 && prevOccupied[tx][ty] == occupied[current.x][current.y]) continue;  // the agents would jump through each other. Not possible.

        distanceGrid[tx][ty] = timestep + 1;
        
        Point child = new Point(tx, ty);
        pending.add(child);
        
      }
    }
    
    return distanceGrid;
  }
  
  private void updateOccupiedTiles(int[][] occupied, int[][] prevOccupied, int timestep) {
    copy(occupied, prevOccupied);
    fill(occupied, -1);
    
    for (Agent agent : memory.agents) {
      Point at;
      
      int lastCommittedIndex = agent.path.size() - 1;
      if (timestep <= lastCommittedIndex) {
        at = agent.path.get(timestep);  // current committed move
      } else {
        Plan plan = memory.getPlan(agent);
        if (plan == null) {
          //at = agent.path.get(lastCommittedIndex); // no plan? use the last known committed move

          // Agents without any committed move at this time-step or any plan, we can request to move. 
          // These are the "conflicting agents" which will try to resolve the newer deadlock.
          continue; 

        } else {  // planned moves
          int offset = lastCommittedIndex;
          int index = timestep - offset;
          
          int lastPlannedIndex = plan.path.size() - 1;
          if (index <= lastPlannedIndex) {
            at = plan.path.get(index);  // current plan move
          } else {
            at = plan.path.get(lastPlannedIndex); // past final move in plan? use the last known planned move.
          }
        }
      }
      
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
  }
}

class TimePoint {
  public int timestep;
  public Point point;
}

class Plan {
  public Agent agent;
  public List<Point> path;
}

class SharedAgentMemory {
  
  public int timestep;  // starts at 0.
  
  public int totalTries;
  public int tryCount;  // share the tryCount between the agents so tries doesn't explode when agents request recursively.
  
  public List<Agent> agents;

  public Board board;
  
  public List<Plan> plans;
  public List<Plan> failedPlans;
  
  public SharedAgentMemory(Board board) {
    this.board = board;
    
    timestep = 0;
    totalTries = 4;
    
    agents = new ArrayList<>();
    
    plans = new ArrayList<>();
    failedPlans = new ArrayList<>();
  }
  
  public Plan getPlan(Agent agent) {
    for (Plan plan : plans) {
      if (plan.agent.equals(agent)) return plan;
    }
    return null;
  }
  
  public List<Plan> getFailedPlans(Agent agent) {
    List<Plan> failed = new ArrayList<>();
    
    for (Plan plan : plans) {
      if (plan.agent.equals(agent)) failed.add(plan);
    }
    
    return failed;
  }
}

class RequestPackage {
  public Agent sender;
  public Agent receiver;
  public Request request;
}

enum ResolveResult {
  ok,
  failed;
}

enum Request {
  resolveDeadlock;
  // moveUp
}
