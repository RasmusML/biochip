package aop;

import java.awt.Canvas;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Point;
import dmb.gui.ColorPalette;
import dmb.helpers.Assert;
import framework.ApplicationAdapter;
import framework.graphics.Alignment;
import framework.graphics.Camera;
import framework.graphics.FitViewport;
import framework.graphics.Renderer;
import framework.input.Button;
import framework.input.Keys;
import framework.math.MathUtils;
import framework.math.Vector2;

public class AopApp extends ApplicationAdapter {

  // graphics
  Renderer renderer;
  FitViewport viewport;
  Camera boardCamera;
  Canvas canvas;

  // mouse
  float oldX, oldY;
  boolean dragging;

  // rendering
  float tilesize = 32f;
  float gap = 1f;

  List<AOPInstance> instances;

  int currentIndex;
  AOPInstance current;

  float dt;

  boolean running;
  boolean step;

  boolean moving;
  float stopTime;
  float movementTime;

  @Override
  public void init() {
    canvas = new Canvas();
    app.setRoot(canvas);
    app.attachInputListenersToComponent(canvas);

    movementTime = 0.12f;
    stopTime = 0.45f;

    canvas.createBufferStrategy(3);
    canvas.setIgnoreRepaint(true);

    viewport = new FitViewport(640, 480, true);
    boardCamera = new Camera();

    renderer = new Renderer(viewport);
    renderer.setCanvas(canvas);

    instances = new ArrayList<>();

    undoTest4();

    pushTest1();

    undoTest6();

    openTest1();
    undoTest3();
    
    reverseTest2();
    normalTest2();
    okTest();
    reverseTest();
    undoTest2();
    undoTest1();
    undoTest5();
    failingTest1();
    failingTest2();
    normalTest3();
    failingTest3();

    //openTest2();
    normalTest1(); // special: requester re-pathing

    Assert.that(instances.size() > 0);
    currentIndex = 0;

    current = instances.get(currentIndex);

    Board board = current.board;
    float cx = board.getWidth() * tilesize / 2f;
    float cy = board.getHeight() * tilesize / 2f;

    boardCamera.lookAtNow(cx, cy);
  }

  private void pushTest1() {
    String layout = 
        "100\n" +
        "101\n" +
        "111\n" +
        "101\n" +
        "111";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 2));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));
    path.add(new Point(1, 0));
    path.add(new Point(2, 0));
    path.add(new Point(2, 1));
    path.add(new Point(2, 2));
    path.add(new Point(2, 3));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void okTest() {
    String layout = 
        "100\n" +
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4), new Point(0, 4), new Point(0, 4), new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));

    agents.add(agent0);
    agents.add(agent1);

    memory.agents.add(agent0);
    memory.agents.add(agent1);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 1));
    path.add(new Point(0, 2));
    path.add(new Point(0, 3));
    path.add(new Point(0, 4));

    Plan plan = agent1.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent1.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private int getExecutionTime(List<Agent> agents) {
    int maxExecutionTime = 0;
    for (Agent agent : agents) {
      int executionTime = agent.getPath().size();
      if (executionTime > maxExecutionTime) maxExecutionTime = executionTime;
    }

    return maxExecutionTime;
  }

  private void normalTest2() {
    String layout = 
        "100\n" +
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));
    Agent agent2 = new Agent(memory, 2, new Point(0, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 1));
    path.add(new Point(0, 2));
    path.add(new Point(0, 3));
    path.add(new Point(0, 4));

    Plan plan = agent1.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent1.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void normalTest3() {
    String layout = 
        "100\n" +
        "111\n" +
        "100\n" +
        "110\n" +
        "100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 3));
    Agent agent1 = new Agent(memory, 1, new Point(0, 1));
    Agent agent2 = new Agent(memory, 2, new Point(1, 1));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void reverseTest() {
    String layout = 
        "100\n" +
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));

    agents.add(agent0);
    agents.add(agent1);

    memory.agents.add(agent0);
    memory.agents.add(agent1);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void reverseTest2() {
    String layout = 
        "100\n" +
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));
    Agent agent2 = new Agent(memory, 2, new Point(0, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void undoTest1() {
    String layout = 
        "0100\n" +
        "0111\n" +
        "0100\n" +
        "1100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 1));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void undoTest2() {
    String layout = 
        "001\n" +
        "111\n" +
        "101\n" +
        "111";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 2));
    Agent agent1 = new Agent(memory, 1, new Point(1, 2));
    Agent agent2 = new Agent(memory, 2, new Point(2, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));
    path.add(new Point(1, 0));
    path.add(new Point(2, 0));
    path.add(new Point(2, 1));
    path.add(new Point(2, 2));
    path.add(new Point(2, 3));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    agent0.request(plan);

    createAOPInstance(board, agents, path);
  }

  private void undoTest3() {
    String layout = 
        "100\n" +
        "101\n" +
        "111\n" +
        "101\n" +
        "111";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 2));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));
    path.add(new Point(1, 0));
    path.add(new Point(2, 0));
    path.add(new Point(2, 1));
    path.add(new Point(2, 2));
    path.add(new Point(2, 3));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void undoTest4() {
    String layout = 
        "0100\n" +
        "0111\n" +
        "0100\n" +
        "1100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 1));
    Agent agent3 = new Agent(memory, 3, new Point(1, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    agents.add(agent3);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    memory.agents.add(agent3);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void undoTest5() {
    String layout = 
        "0100\n" +
        "0111\n" +
        "0100\n" +
        "1100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));
    Agent agent3 = new Agent(memory, 3, new Point(1, 1));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    agents.add(agent3);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    memory.agents.add(agent3);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void undoTest6() {
    String layout = 
        "1000\n" +
        "1001\n" +
        "1111\n" +
        "1001\n" +
        "1111";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 2));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));
    Agent agent3 = new Agent(memory, 3, new Point(2, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    agents.add(agent3);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    memory.agents.add(agent3);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));
    path.add(new Point(1, 0));
    path.add(new Point(2, 0));
    path.add(new Point(3, 0));
    path.add(new Point(3, 1));
    path.add(new Point(3, 2));
    path.add(new Point(3, 3));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  private void failingTest1() {
    String layout = 
        "0100\n" +
        "0110\n" +
        "0100\n" +
        "0100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.failed);

    createAOPInstance(board, agents, path);
  }

  private void failingTest2() {
    String layout = 
        "0100\n" +
        "0110\n" +
        "0100\n" +
        "1100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));
    Agent agent3 = new Agent(memory, 3, new Point(1, 1));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    agents.add(agent3);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    memory.agents.add(agent3);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.failed);

    createAOPInstance(board, agents, path);
  }

  private void failingTest3() {
    String layout = 
        "0100\n" +
        "0111\n" +
        "0100\n" +
        "1100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 2));
    Agent agent3 = new Agent(memory, 3, new Point(1, 1));
    Agent agent4 = new Agent(memory, 4, new Point(0, 1));

    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    agents.add(agent3);
    agents.add(agent4);

    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    memory.agents.add(agent3);
    memory.agents.add(agent4);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.failed);

    createAOPInstance(board, agents, path);
  }

  // only possible to solve, if requester can use other cells than that of the route to resolve the deadlock
  private void normalTest1() {
    String layout = 
        "0100\n" +
        "0110\n" +
        "0100\n" +
        "0100\n" +
        "0100";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(1, 3));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));

    agents.add(agent0);
    agents.add(agent1);

    memory.agents.add(agent0);
    memory.agents.add(agent1);

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent0.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent0.request(plan);
    Assert.that(result == ResolveResult.failed);

    createAOPInstance(board, agents, path);
  }

  private void openTest1() {
    String layout = 
        "111\n" +
        "111\n" +
        "101\n" +
        "111\n" +
        "111";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 1), new Point(0, 2));
    Agent agent1 = new Agent(memory, 1, new Point(0, 4));

    agents.add(agent0);
    agents.add(agent1);

    memory.agents.add(agent0);
    memory.agents.add(agent1);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent1.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent1.request(plan);
    Assert.that(result == ResolveResult.ok);

    createAOPInstance(board, agents, path);
  }

  // figure out what to do here. The problem is the requester has selected a path which goes through another agents committed move. We should probably make it illegal to do so, but not handle it in this algorith. Maybe just detect it?
  private void openTest2() {
    String layout = 
        "111\n" +
        "111\n" +
        "101\n" +
        "111\n" +
        "111";

    Board board = new Board(layout);

    List<Agent> agents = new ArrayList<>();
    SharedAgentMemory memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 3), new Point(0, 2), new Point(0, 2), new Point(0, 2));
    Agent agent1 = new Agent(memory, 1, new Point(0, 4));

    agents.add(agent0);
    agents.add(agent1);

    memory.agents.add(agent0);
    memory.agents.add(agent1);

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));

    Plan plan = agent1.myPlan;
    plan.addToPlan(path);

    ResolveResult result = agent1.request(plan);
    Assert.that(result == ResolveResult.failed);

    createAOPInstance(board, agents, path);
  }

  private void createAOPInstance(Board board, List<Agent> agents, List<Point> path) {
    AOPInstance instance = new AOPInstance();
    instance.timestamp = 0;
    instance.executionTime = getExecutionTime(agents);
    instance.agents = agents;
    instance.board = board;
    instance.originalRequesterPath = new ArrayList<>(path);

    instances.add(instance);
  }

  @Override
  public void update() {
    handleInput();

    String title = String.format("@%d", app.getFps());
    app.setTitle(title);

    if (running) step = true;

    if (current.timestamp < current.executionTime) {
      if (step) {
        float maxTime = moving ? movementTime : stopTime;

        dt += app.getDelta() / 1000f;
        dt = MathUtils.clamp(0, maxTime, dt);

        if (dt == maxTime) {
          dt = 0;

          if (moving) {
            current.timestamp += 1;
            step = false;
          }

          moving = !moving;
        }
      }
    }
  }

  private void handleInput() {
    if (input.isKeyPressed(Keys.X)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 1.02f);
    }

    if (input.isKeyPressed(Keys.Z)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 0.98f);
    }

    if (input.isKeyJustPressed(Keys.SPACE)) {
      if (running) {
        if (!moving) { // if the droplets are not in the moving state, then just stop immediately. Otherwise, let them finish the move. Yielding a smooth animation
          step = false;
          dt = 0;
        }
      } else {
        // start at move-state, so they move instantly.
        moving = true;
      }

      running = !running;
    }

    if (input.isKeyJustPressed(Keys.W)) {
      running = false;
      moving = false;
      step = false;
      dt = 0;

      currentIndex += 1;
      if (currentIndex >= instances.size()) currentIndex = instances.size() - 1;

      current = instances.get(currentIndex);

      Board board = current.board;
      float cx = board.getWidth() * tilesize / 2f;
      float cy = board.getHeight() * tilesize / 2f;

      boardCamera.lookAtNow(cx, cy);
    }

    if (input.isKeyJustPressed(Keys.Q)) {
      running = false;
      moving = false;
      step = false;
      dt = 0;

      currentIndex -= 1;
      if (currentIndex < 0) currentIndex = 0;

      current = instances.get(currentIndex);

      Board board = current.board;
      float cx = board.getWidth() * tilesize / 2f;
      float cy = board.getHeight() * tilesize / 2f;

      boardCamera.lookAtNow(cx, cy);
    }

    if (input.isKeyJustPressed(Keys.R)) {
      running = false;
      step = false;
      current.timestamp = 0;
      dt = 0;
    }

    viewport.setCamera(boardCamera);

    if (input.isMouseJustPressed(Button.RIGHT)) {
      dragging = true;

      Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
      oldX = mouse.x;
      oldY = mouse.y;
    }

    if (input.isMouseJustReleased(Button.RIGHT)) {
      dragging = false;
    }

    if (dragging) {
      Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
      float mouseX = mouse.x;
      float mouseY = mouse.y;

      float dx = mouseX - oldX;
      float dy = mouseY - oldY;

      float tx = boardCamera.x - dx;
      float ty = boardCamera.y - dy;

      boardCamera.lookAtNow(tx, ty);
    }

    if (input.isKeyJustPressed(Keys.RIGHT)) {
      running = false;
      step = false;
      current.timestamp += 1;
    }

    if (input.isKeyJustPressed(Keys.LEFT)) {
      running = false;
      step = false;
      dt = 0;

      current.timestamp -= 1;
      if (current.timestamp < 0) current.timestamp = 0;
    }
  }

  @Override
  public void draw() {
    boardCamera.update();

    renderer.begin();
    renderer.clear();

    drawBoard();

    renderer.end();
  }

  private void drawBoard() {
    viewport.setCamera(boardCamera);

    Board board = current.board;

    { // frame
      renderer.setColor(Color.GRAY);

      float xx = -gap;
      float yy = -gap;
      float width = board.getWidth() * tilesize + gap * 2f;
      float height = board.getHeight() * tilesize + gap * 2f;

      renderer.fillRect(xx, yy, width, height);

      // id
      float offsetX = -5;
      float offsetY = 5;

      float xxx = xx + offsetX;
      float yyy = yy + offsetY + height;

      String text = String.format("%d", currentIndex);

      renderer.setColor(Color.black);
      renderer.drawText(text, xxx, yyy, Alignment.BottomLeft);
    }

    { // grid tiles
      for (int x = 0; x < board.getWidth(); x++) {
        for (int y = 0; y < board.getHeight(); y++) {
          int tile = board.grid[x][y];

          float xx = x * tilesize + gap;
          float yy = y * tilesize + gap;

          float width = tilesize - gap * 2f;
          float height = tilesize - gap * 2f;

          if (tile == 1) {
            renderer.setColor(Color.WHITE);
          } else {
            renderer.setColor(Color.gray);
          }

          renderer.fillRect(xx, yy, width, height);
        }
      }
    }

    { // agents

      for (Agent agent : current.agents) {
        drawAgent(agent);
      }
    }

    { // draw path
      for (Point path : current.originalRequesterPath) {
        float radius = tilesize / 4f;

        float xx = path.x * tilesize + gap + (tilesize - gap * 2f) / 2f - radius;
        float yy = path.y * tilesize + gap + (tilesize - gap * 2f) / 2f - radius;

        Color color = ColorPalette.seeThroughRed;
        renderer.setColor(color);
        renderer.fillCircle(xx, yy, radius);

      }
    }
  }

  private void drawAgent(Agent agent) {
    Color color = Color.green;
    Point at = agent.getPosition(current.timestamp);

    if (at == null) {
      at = agent.getPosition();
      color = Color.gray;
    }

    float dx = 0;
    float dy = 0;

    if (moving) {
      Point to = agent.getPosition(current.timestamp + 1);
      if (to == null) {
        to = agent.getPosition();
      }

      dx = to.x - at.x;
      dy = to.y - at.y;
    }

    float offset = (tilesize - tilesize) / 2f;

    float percentage = dt / (float) movementTime;

    float x = (at.x + dx * percentage) * tilesize + gap + offset;
    float y = (at.y + dy * percentage) * tilesize + gap + offset;

    float width = tilesize - gap * 2f;
    float height = tilesize - gap * 2f;

    renderer.setColor(color);

    renderer.fillOval(x, y, width, height);

    String idString = String.format("%d", agent.getId());

    float tx = x + width / 2f;
    float ty = y + height / 2f;

    renderer.setColor(Color.BLACK);
    renderer.drawText(idString, tx, ty, Alignment.Center);

    renderer.drawOval(x, y, width, height);
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(canvas.getWidth(), canvas.getHeight());
  }
}

class AOPInstance {
  public int timestamp;
  public int executionTime;

  public List<Point> originalRequesterPath;

  public Board board;
  public SharedAgentMemory memory;
  public List<Agent> agents;

}