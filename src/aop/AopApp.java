package aop;

import java.awt.Canvas;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Point;
import framework.ApplicationAdapter;
import framework.graphics.Alignment;
import framework.graphics.Camera;
import framework.graphics.FitViewport;
import framework.graphics.Renderer;
import framework.input.Button;
import framework.input.Keys;
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

  Board board;
  int timestamp;
  
  float dt;

  boolean running;
  boolean step;
  
  boolean moving;
  float stopTime;
  float movementTime;

  List<Agent> agents;
  SharedAgentMemory memory;
  
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
    
    /*
    okTest();
    reverseTest();
    normalTest2();
    reverseTest2();
    */
    undoTest1();
    
    float cx = board.getWidth() * tilesize / 2f;
    float cy = board.getHeight() * tilesize / 2f;
    boardCamera.lookAtNow(cx, cy);
  }

  private void okTest() {
    String layout = "100\n" + 
                    "111\n" +
                    "100\n" +
                    "100\n" +
                    "100";
    
    board = new Board(layout);
    
    agents = new ArrayList<>();

    memory = new SharedAgentMemory(board);

    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));
    
    agents.add(agent0);
    agents.add(agent1);
    
    memory.agents.add(agent0);
    memory.agents.add(agent1);
    
    memory.start();

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 1));
    path.add(new Point(0, 2));
    path.add(new Point(0, 3));
    path.add(new Point(0, 4));
    
    Plan plan = memory.getPlan(agent1);
    plan.addToPlan(path);
    
    agent1.request(plan);
  }
  
  private void normalTest2() {
    String layout = 
        "100\n" + 
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    board = new Board(layout);
    
    agents = new ArrayList<>();
    
    memory = new SharedAgentMemory(board);
    
    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));
    Agent agent2 = new Agent(memory, 2, new Point(0, 2));
    
    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    
    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    
    memory.start();

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 1));
    path.add(new Point(0, 2));
    path.add(new Point(0, 3));
    path.add(new Point(0, 4));
    
    Plan plan = memory.getPlan(agent1);
    plan.addToPlan(path);
    
    agent1.request(plan);
  }

  private void reverseTest() {
    String layout = 
        "100\n" + 
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    board = new Board(layout);
    
    agents = new ArrayList<>();

    memory = new SharedAgentMemory(board);
    
    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));
    
    agents.add(agent0);
    agents.add(agent1);
    
    memory.agents.add(agent0);
    memory.agents.add(agent1);
    
    memory.start();

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));
    
    Plan plan = memory.getPlan(agent0);
    plan.addToPlan(path);
    
    agent0.request(plan);
  }
  
  private void reverseTest2() {
    String layout = 
        "100\n" + 
        "111\n" +
        "100\n" +
        "100\n" +
        "100";

    board = new Board(layout);
    
    agents = new ArrayList<>();

    memory = new SharedAgentMemory(board);
    
    Agent agent0 = new Agent(memory, 0, new Point(0, 4));
    Agent agent1 = new Agent(memory, 1, new Point(0, 0));
    Agent agent2 = new Agent(memory, 2, new Point(0, 2));
    
    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    
    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    
    memory.start();

    List<Point> path = new ArrayList<>();
    path.add(new Point(0, 3));
    path.add(new Point(0, 2));
    path.add(new Point(0, 1));
    path.add(new Point(0, 0));
    
    Plan plan = memory.getPlan(agent0);
    plan.addToPlan(path);
    
    agent0.request(plan);
  }
  
  private void undoTest1() {
    String layout = 
        "0100\n" + 
        "0111\n" +
        "0100\n" +
        "1100\n" +
        "0100";

    board = new Board(layout);
    
    agents = new ArrayList<>();

    memory = new SharedAgentMemory(board);
    
    Agent agent0 = new Agent(memory, 0, new Point(1, 4));
    Agent agent1 = new Agent(memory, 1, new Point(1, 0));
    Agent agent2 = new Agent(memory, 2, new Point(1, 1));
    
    agents.add(agent0);
    agents.add(agent1);
    agents.add(agent2);
    
    memory.agents.add(agent0);
    memory.agents.add(agent1);
    memory.agents.add(agent2);
    
    memory.start();

    List<Point> path = new ArrayList<>();
    path.add(new Point(1, 3));
    path.add(new Point(1, 2));
    path.add(new Point(1, 1));
    path.add(new Point(1, 0));
    
    Plan plan = memory.getPlan(agent0);
    plan.addToPlan(path);
    
    agent0.request(plan);
  }

  @Override
  public void update() {
    if (running) step = true;
    
    /*
    if (timestamp < result.executionTime) {
      if (step) {
        float maxTime = moving ? movementTime : stopTime;
  
        dt += app.getDelta() / 1000f;
        dt = MathUtils.clamp(0, maxTime, dt);
        
        if (dt == maxTime) {
          dt = 0;
  
          if (moving) {
            timestamp += 1;
            step = false;
          }
          
          moving = !moving;
        }
      }
    }
    */
    
    String title = String.format("@%d", app.getFps());
    app.setTitle(title);

    if (input.isKeyPressed(Keys.X)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 1.02f);
    }
    
    if (input.isKeyPressed(Keys.Z)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 0.98f);
    }
    
    if (input.isKeyJustPressed(Keys.SPACE)) {
      //reshaper.step();
      
      /*
      if (running) {
        if (!moving) {  // if the droplets are not in the moving state, then just stop immediately. Otherwise, let them finish the move. Yielding a smooth animation
          step = false;
          dt = 0;
        }
      } else {
        // start at move-state, so they move instantly.
        moving = true;
      }
      
      running = !running;
      */
      
    }
    
    if (input.isKeyJustPressed(Keys.R)) {
      running = false;
      step = false;
      timestamp = 0;
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
      timestamp += 1;
    }
    
    if (input.isKeyJustPressed(Keys.LEFT)) {
      running = false;
      step = false;
      dt = 0;

      timestamp -= 1;
      if (timestamp < 0) timestamp = 0;
    }

    boardCamera.update();
  }

  @Override
  public void draw() {
    renderer.begin();
    renderer.clear();

    drawBoard();
    
    renderer.end();
  }

  private void drawBoard() {
    viewport.setCamera(boardCamera);
    
    { // frame
      renderer.setColor(Color.GRAY);

      float xx = -gap;
      float yy = -gap;
      float width = board.getWidth() * tilesize + gap * 2f;
      float height = board.getHeight() * tilesize + gap * 2f;

      renderer.fillRect(xx, yy, width, height);
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
      
      for (Agent agent : agents) {
        drawAgent(agent, Color.green);
      }
    }
  }

  private void drawAgent(Agent agent, Color color) {
    Point at = agent.getPosition(timestamp);
    if (at == null) return;
    
    float offset = (tilesize - tilesize) / 2f;
    
    float x = at.x * tilesize + gap + offset;
    float y = at.y * tilesize + gap + offset;
    
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
