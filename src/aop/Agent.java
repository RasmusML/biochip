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
      int[][] map = getResolvingMap();
      
      print(map);
      
      List<Plan> failedPlans = memory.getFailedPlans(this);
      List<Point> resolvedPath = getResolvingPath(failedPlans, map);

      if (resolvedPath == null) return ResolveResult.failed;
      
      Plan plan = new Plan();
      plan.agent = this;
      plan.path = resolvedPath;
      memory.plans.add(plan);
      
      List<Agent> conflictingAgents = getConflictingAgents(resolvedPath);
      
      Assert.that(conflictingAgents.size() == 0, "not ready to test conflicting agents yet.");
      
      for (Agent agent : conflictingAgents) {
        RequestPackage newPack = new RequestPackage();
        newPack.sender = this;
        newPack.receiver = agent;
        newPack.request = Request.resolveDeadlock;
        
        ResolveResult result = agent.resolve(newPack);
        if (result != ResolveResult.ok) return ResolveResult.failed;  // @TODO: update memory.plans to contain failed?
      }
      
      // commit
      path.addAll(plan.path);

      memory.plans.remove(plan);
      memory.failedPlans.add(plan);
      
      return ResolveResult.ok;
      
    } else {
      throw new IllegalStateException("unknown request!");
    }
  }

  private List<Agent> getConflictingAgents(List<Point> path) {
    List<Agent> conflicting = new ArrayList<>();
    
    // @TODO
    
    return conflicting;
  }

  private List<Point> getResolvingPath(List<Plan> failedPlans, int[][] map) {
    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] overlay = new int[width][height];
    copy(map, overlay);
    
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
        if (overlay[x][y] < 0) continue;
        
        Point endpoint = new Point(x, y);
        endPoints.add(endpoint);
      }
    }

    List<Point> longPath = new ArrayList<>();
    while (endPoints.size() > 0) {
      Point endPoint = endPoints.remove(0);
      longPath.add(endPoint);
      
      // traverse
      Point current = endPoint;
      while (map[current.x][current.y] > 0) {
        
        Point backtrackMove = backtrack(current, map, overlay);
        if (backtrackMove == null) break;
        
        Point to = current.copy().add(backtrackMove.x, backtrackMove.y);
        longPath.add(to);
        current = to;
      }
      
      // if no path exists for the endpoint retry with another endpoint
      Point startPoint = longPath.get(longPath.size() - 1);
      if (map[startPoint.x][startPoint.y] == 0) break;
      
      longPath.clear();
    }
    
    if (longPath.size() == 0) return null;  // no valid path.
    
    Collections.reverse(longPath);

    List<Point> path = new ArrayList<>();
    for (Point point : longPath) {
      path.add(point);
      
      if (overlay[point.x][point.y] >= 0) break;
    }
    
    path.remove(0); // remove the first point, because the agent is already there.
    
    return path;
  }


  private Point backtrack(Point current, int[][] map, int[][] overlay) {
    Board board = memory.board;
    
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    int cost = map[current.x][current.y];

    for (Point move : moves) {
      int tx = current.x + move.x;
      int ty = current.y + move.y;
      
      if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
      
      int newCost = map[tx][ty];
      if (newCost == cost - 1) return move;
    }
    
    return null;
  }

  private int[][] getResolvingMap() {
    Assert.that(path.size() - 1 == memory.timestep);

    Board board = memory.board;
    
    int width = board.getWidth();
    int height = board.getHeight();
    
    int[][] map = new int[width][height];
    
    int[][] prevOccupied = new int[width][height];
    int[][] occupied = new int[width][height];
    
    fill(occupied, -1);
    fill(map, -1);
    
    List<Point> moves = new ArrayList<>();
    moves.add(new Point(-1, 0));
    moves.add(new Point(1, 0));
    moves.add(new Point(0, 1));
    moves.add(new Point(0, -1));
    
    Point at = path.get(memory.timestep);
    map[at.x][at.y] = 0;

    List<Point> pending = new ArrayList<>();
    pending.add(at);
    
    int timestep = memory.timestep;
    updateOccupiedTiles(occupied, prevOccupied, timestep);
    
    // flood-fill algorithm
    while (pending.size() > 0) {
      Point current = pending.remove(0);
      
      int time = map[current.x][current.y];
      if (time > timestep) {
        timestep = time;
        updateOccupiedTiles(occupied, prevOccupied, timestep);
      }
      
      for (Point move : moves) {
        int tx = current.x + move.x;
        int ty = current.y + move.y;
        
        if (!board.isTileOpen(tx, ty)) continue;  // wall or outside the board
        if (map[tx][ty] != -1) continue;  // already visited
        
        if (occupied[tx][ty] != -1) continue; // already occupied
        if (prevOccupied[tx][ty] != -1 && prevOccupied[tx][ty] == occupied[current.x][current.y]) continue;  // they would jump through each other.

        map[tx][ty] = timestep + 1;
        
        Point child = new Point(tx, ty);
        pending.add(child);
        
      }
    }
    
    return map;
  }
  
  private void updateOccupiedTiles(int[][] occupied, int[][] prevOccupied, int timestep) {
    copy(occupied, prevOccupied);
    fill(occupied, -1);
    
    // committed moves
    for (Agent agent : memory.agents) {
      int lastIndex = agent.path.size() - 1;

      Point at;
      if (timestep <= lastIndex) at = agent.path.get(timestep);
      else at = agent.path.get(lastIndex);
      
      int id = agent.getId();
      occupied[at.x][at.y] = id;
    }
    
    // planed moves
    for (Plan plan : memory.plans) {
      int offset = plan.agent.path.size() - 1;
      int index = timestep - offset;
      if (index < 0) continue;
      
      int lastIndex = plan.path.size() - 1;

      Point at;
      if (index <= lastIndex) at = plan.path.get(index);
      else at = plan.path.get(lastIndex);
      
      int id = plan.agent.getId();
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

  private void print(int[][] array) {
    for (int x = 0; x < array.length; x++) {
      for (int y = 0; y < array[0].length; y++) {
        System.out.printf(array[x][y] + " ");
      }
      
      System.out.printf("\n");
    }
  }
  
  private void fill(int[][] array, int value) {
    for (int x = 0; x < array.length; x++) {
      for (int y = 0; y < array[0].length; y++) {
        array[x][y] = value;
      }
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
  
  public List<Agent> agents;

  public Board board;
  
  public List<Plan> plans;
  public List<Plan> failedPlans;
  
  public SharedAgentMemory(Board board) {
    this.board = board;
    
    timestep = 0;
    
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
  //defer,
  failed;
}

enum Request {
  resolveDeadlock;
  // moveUp
}
