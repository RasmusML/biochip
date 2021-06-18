package dmb.reshaping;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.BioArray;
import dmb.algorithms.Point;
import dmb.testbench.tests.Test1BioArray;
import engine.ApplicationAdapter;
import engine.graphics.Alignment;
import engine.graphics.Camera;
import engine.graphics.FitViewport;
import engine.graphics.Renderer;
import engine.input.Button;
import engine.input.Keys;
import engine.math.Vector2;

public class DropletReshapeApp extends ApplicationAdapter {

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

  BioArray array;
  int timestamp;
  
  float dt;

  boolean running;
  boolean step;
  
  boolean moving;
  float stopTime;
  float movementTime;
  
  DropletReshapeSimulator reshaper;
  ShapedDroplet droplet;
  
  List<Point> reshape;
  
  @Override
  public void init() {
    canvas = new Canvas();
    app.setRoot(canvas);
    app.attachInputListenersToComponent(canvas);
    
    movementTime = 0.12f;
    stopTime = 0.45f;

    reshape = new ArrayList<>();
    
    canvas.createBufferStrategy(3);
    canvas.setIgnoreRepaint(true);

    viewport = new FitViewport(640, 480, true);
    boardCamera = new Camera();
    
    renderer = new Renderer(viewport);
    renderer.setCanvas(canvas);
    
    
    array = new Test1BioArray();
    
    float cx = array.width * tilesize / 2f;
    float cy = array.height * tilesize / 2f;
    boardCamera.lookAtNow(cx, cy);
    
    reshaper = new DropletReshapeSimulator();
    
    droplet = new ShapedDroplet();
    droplet.points = new ArrayList<>();
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
      reshaper.step();
      
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

    if (input.isMouseJustReleased(Button.LEFT)) {
      int mx = input.getX();
      int my = input.getY();
      
      Vector2 world = viewport.screenToWorld(mx, my);
      
      int x = (int) (world.x / tilesize);
      int y = (int) (world.y / tilesize);
      
      Point point = new Point(x, y);
      
      if (droplet.points.contains(point)) {
        droplet.points.remove(point);
      } else {
        droplet.points.add(point);
      }
      
      if (reshape.size() == droplet.points.size()) {
        reshaper.reshape(droplet, reshape);
      }
    }

    viewport.setCamera(boardCamera);

    if (input.isMouseJustPressed(Button.RIGHT)) {
      int mx = input.getX();
      int my = input.getY();
      
      Vector2 world = viewport.screenToWorld(mx, my);
      
      int x = (int) (world.x / tilesize);
      int y = (int) (world.y / tilesize);
      
      Point point = new Point(x, y);
      
      if (reshape.contains(point)) {
        reshape.remove(point);
      } else {
        reshape.add(point);
      }
      
      if (reshape.size() == droplet.points.size()) {
        reshaper.reshape(droplet, reshape);
      }
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
      float width = array.width * tilesize + gap * 2f;
      float height = array.height * tilesize + gap * 2f;

      renderer.fillRect(xx, yy, width, height);
    }

    { // grid tiles
      renderer.setColor(Color.WHITE);
      for (int x = 0; x < array.width; x++) {
        for (int y = 0; y < array.height; y++) {

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
  }

  private void drawDroplet(ShapedDroplet droplet) {
    List<Point> points = droplet.points;
    for (int i = 0; i < points.size(); i++) {
      Point at = points.get(i);
      
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
