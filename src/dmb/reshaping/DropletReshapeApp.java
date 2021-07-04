package dmb.reshaping;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import dmb.actuation.ElectrodeActivationTranslator;
import dmb.actuation.ElectrodeActivations;
import dmb.actuation.ElectrodeActuation;
import dmb.actuation.ElectrodeState;
import dmb.algorithms.Point;
import dmb.components.moves.Move;
import dmb.helpers.GeometryUtil;
import dmb.platform.PlatformInterface;
import framework.ApplicationAdapter;
import framework.graphics.Alignment;
import framework.graphics.Camera;
import framework.graphics.FitViewport;
import framework.graphics.Renderer;
import framework.input.Button;
import framework.input.Droplet;
import framework.input.DropletUnit;
import framework.input.Keys;
import framework.math.Vector2;

public class DropletReshapeApp extends ApplicationAdapter {

  boolean sendCommandsToPlatform = false;
  
  boolean firstCommandSend;
  
  boolean reshaping;

  // graphics
  Renderer renderer;
  FitViewport viewport;
  Camera boardCamera;
  Canvas canvas;

  // rendering
  float tilesize = 32f;
  float gap = 1f;

  int timestamp;

  float dt;

  boolean running;
  boolean step;

  boolean moving;
  float stopTime;
  float movementTime;

  int gridWidth = 32;
  int gridHeight = 20;

  DropletReshapeSimulator reshaper;
  Droplet droplet;
  
  Point hovered;

  List<Point> reshape;

  PlatformInterface pi;

  @Override
  public void init() {
    canvas = new Canvas();
    app.setRoot(canvas);
    app.attachInputListenersToComponent(canvas);
    
    firstCommandSend = false;

    movementTime = 0.12f;
    stopTime = 0.45f;

    reshape = new ArrayList<>();

    canvas.createBufferStrategy(3);
    canvas.setIgnoreRepaint(true);
    
    reshaping = false;
    
    hovered = new Point();

    viewport = new FitViewport(640, 480, true);
    boardCamera = new Camera();

    renderer = new Renderer(viewport);
    renderer.setCanvas(canvas);

    if (sendCommandsToPlatform) {
      pi = new PlatformInterface();
      pi.connect();
      pi.setHighVoltageValue(299);
      pi.turnHighVoltageOnForElectrodes();
      pi.clearAllElectrodes();
    }

    float cx = gridWidth * tilesize / 2f;
    float cy = gridHeight * tilesize / 2f;
    boardCamera.lookAtNow(cx, cy);
    
    boardCamera.zoomNow(0.6f);

    reshaper = new DropletReshapeSimulator(gridWidth, gridHeight);

    droplet = new Droplet();
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

    if (input.isKeyJustPressed(Keys.Q)) {
      if (sendCommandsToPlatform) {
        pi.disconnect();
      }
    }

    if (input.isKeyPressed(Keys.X)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 1.02f);
    }
    
    if (input.isKeyJustPressed(Keys.UP)) {
      move(Move.Up);
    }
    
    if (input.isKeyJustPressed(Keys.DOWN)) {
      move(Move.Down);
    }
    
    if (input.isKeyJustPressed(Keys.LEFT)) {
      move(Move.Left);
    }

    if (input.isKeyJustPressed(Keys.RIGHT)) {
      move(Move.Right);
    }
    
    if (input.isKeyPressed(Keys.Z)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 0.98f);
    }

    if (input.isKeyJustPressed(Keys.SPACE)) {
      if (reshaping) {
        reshaper.step();
        sendCommandsOfCurrentPositions();
      }
    }

    if (input.isKeyJustPressed(Keys.R)) {
      reshape.clear();

      reshaping = false;
      
      running = false;
      step = false;
      timestamp = 0;
      dt = 0;
    }

    viewport.setCamera(boardCamera);

    int mx = input.getX();
    int my = input.getY();
    
    Vector2 world = viewport.screenToWorld(mx, my);
    
    int x = (int) (world.x / tilesize);
    int y = (int) (world.y / tilesize);
    
    hovered = new Point();
    hovered.x = x;
    hovered.y = y;
    
    if (input.isMouseJustReleased(Button.LEFT)) {
      if (reshape.size() == 0) {
        if (GeometryUtil.inside(hovered.x, hovered.y, gridWidth, gridHeight)) {
          boolean match = false;
          for (DropletUnit unit : droplet.units) {
            Point at = unit.route.getPosition();

            if (x == at.x && y == at.y) {
              droplet.units.remove(unit);
              match = true;
              break;
            }
          }

          if (!match) {
            DropletUnit unit = new DropletUnit();
            unit.route.path.add(new Point(x, y));
            droplet.units.add(unit);
          }
        }
      }
    }

    if (input.isMouseJustPressed(Button.RIGHT)) {
      
      if (GeometryUtil.inside(hovered.x, hovered.y, gridWidth, gridHeight)) {
        Point point = new Point(x, y);

        if (reshape.contains(point)) {
          reshape.remove(point);
        } else {
          
          if (reshape.size() < droplet.units.size()) {
            reshape.add(point);
          }
  
          if (reshape.size() == droplet.units.size()) {
            reshaping = true;
            
            reshaper.reshape(droplet, reshape);
  
            if (!firstCommandSend) {
              sendCommandsOfCurrentPositions();
              firstCommandSend = true;
            }
          }
        }
      }
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

  private void sendCommandsOfCurrentPositions() {
    if (!sendCommandsToPlatform) return;
    
    List<Droplet> droplets = new ArrayList<>();
    droplets.add(droplet);

    DropletUnit unit = droplet.units.get(0);
    int executionTime = unit.route.path.size();

    ElectrodeActivationTranslator translator = new ElectrodeActivationTranslator(gridWidth, gridHeight);
    ElectrodeActivations[] sections = translator.translateStateful(droplets, executionTime);

    int last = sections.length - 1;

    ElectrodeActivations section = sections[last];

    for (ElectrodeActuation actuation : section.activations) {
      Point tile = actuation.tile;

      if (actuation.state == ElectrodeState.On) {
        pi.setElectrode(tile.x, tile.y);
      }
    }

    for (ElectrodeActuation actuation : section.activations) {
      Point tile = actuation.tile;
      
      if (actuation.state == ElectrodeState.Off) {
        pi.clearElectrode(tile.x, tile.y);
      }
    }
    
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
      float width = gridWidth * tilesize + gap * 2f;
      float height = gridHeight * tilesize + gap * 2f;

      renderer.fillRect(xx, yy, width, height);
    }

    { // grid tiles
      renderer.setColor(Color.WHITE);
      for (int x = 0; x < gridWidth; x++) {
        for (int y = 0; y < gridHeight; y++) {

          float xx = x * tilesize + gap;
          float yy = y * tilesize + gap;

          float width = tilesize - gap * 2f;
          float height = tilesize - gap * 2f;

          renderer.fillRect(xx, yy, width, height);
        }
      }
    }

    { // droplets

      drawDroplet(droplet);
      drawReshape();
    }
    
    
    String coords = String.format("(%d,%d)", hovered.x, hovered.y);
    renderer.setFont("consolas", Font.PLAIN, 22);
    renderer.setColor(Color.black);
    renderer.drawText(coords, -20, -20, Alignment.BottomLeft);
  }
  
  private void move(Move move) {
    for (DropletUnit unit : droplet.units) {
      Point to = unit.route.getPosition().copy();
      to.add(move.x, move.y);
      unit.route.path.add(to);
    }
    
    sendCommandsOfCurrentPositions();
  }

  private void drawDroplet(Droplet droplet) {
    for (int i = 0; i < droplet.units.size(); i++) {
      DropletUnit unit = droplet.units.get(i);
      Point at = unit.route.getPosition();

      drawSingleDroplet(i + 1, at, Color.green, tilesize);
    }
  }

  private void drawReshape() {
    for (int i = 0; i < reshape.size(); i++) {
      Point at = reshape.get(i);

      drawReshapeDroplet(i + 1, at, tilesize / 2);
    }
  }

  private void drawReshapeDroplet(int id, Point at, float size) {
    float offset = (tilesize - size) / 2f;

    float x = at.x * tilesize + gap + offset;
    float y = at.y * tilesize + gap + offset;

    float width = size - gap * 2f;
    float height = size - gap * 2f;

    renderer.setColor(new Color(0, 0, 255, 100));

    renderer.fillOval(x, y, width, height);
    renderer.drawOval(x, y, width, height);
  }

  private void drawSingleDroplet(int id, Point at, Color color, float size) {
    float offset = (tilesize - size) / 2f;

    float x = at.x * tilesize + gap + offset;
    float y = at.y * tilesize + gap + offset;

    float width = size - gap * 2f;
    float height = size - gap * 2f;

    renderer.setColor(color);

    renderer.fillOval(x, y, width, height);

    String idString = String.format("%d", id);

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
