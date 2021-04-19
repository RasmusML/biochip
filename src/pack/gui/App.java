package pack.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.util.List;

import engine.ApplicationAdapter;
import engine.graphics.Alignment;
import engine.graphics.Camera;
import engine.graphics.FitViewport;
import engine.graphics.Renderer;
import engine.input.Button;
import engine.input.Keys;
import engine.math.MathUtils;
import engine.math.Vector2;
import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.DefaultMixingPercentages;
import pack.algorithms.Droplet;
import pack.algorithms.GreedyRouter;
import pack.algorithms.MixingPercentages;
import pack.algorithms.Point;
import pack.tests.Test3BioArray;
import pack.tests.Test3BioAssay;

public class App extends ApplicationAdapter {

	// graphics
	Renderer renderer;
	FitViewport viewport;
	Camera camera;
	Canvas canvas;

	// mouse
	float oldX, oldY;
	boolean dragging;

	// rendering
	float tilesize = 32f;
	float gap = 1f;

	BioArray array;
	BioAssay assay;
	MixingPercentages percentages;
	
	List<Droplet> droplets;
	int timestamp;
	int moveTicks = 0;
	
	float dt;
	float speed = 30*8f;

	@Override
	public void init() {
		canvas = new Canvas();
		app.setRoot(canvas);
		app.attachInputListenersToComponent(canvas);

		canvas.createBufferStrategy(3);
		canvas.setIgnoreRepaint(true);

		viewport = new FitViewport(640, 480, true);
		camera = new Camera();
		renderer = new Renderer(viewport);

		viewport.setCamera(camera);
		renderer.setCanvas(canvas);
		
		assay = new Test3BioAssay();
		array = new Test3BioArray();
		percentages = new DefaultMixingPercentages();
		
		GreedyRouter router = new GreedyRouter();
		droplets = router.compute(assay, array, percentages);

		float cx = array.width * tilesize / 2f;
		float cy = array.height * tilesize / 2f;
		camera.lookAtNow(cx, cy);
	}

	@Override
	public void update() {
	  if (moveTicks > 0) {
	    dt += app.getDelta() / 1000f * speed;
	    dt = MathUtils.clamp(0, tilesize, dt);
	    
	    if (dt == tilesize) {
	      moveTicks -= 1;
	      timestamp += 1;
	      dt = 0;
	    }
	  }
	  
		String title = String.format("@%d", app.getFps());
		app.setTitle(title);

		if (input.isMousePressed(Button.RIGHT)) {
			camera.zoomNow(camera.targetZoom * 1.02f);
		}

		if (input.isMouseJustPressed(Button.LEFT)) {
			dragging = true;

			Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
			oldX = mouse.x;
			oldY = mouse.y;
		}

		if (input.isMouseJustReleased(Button.LEFT)) {
			dragging = false;
		}

		if (dragging) {
			Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
			float mouseX = mouse.x;
			float mouseY = mouse.y;

			float dx = mouseX - oldX;
			float dy = mouseY - oldY;

			float tx = camera.x - dx;
			float ty = camera.y - dy;

			camera.lookAtNow(tx, ty);
		}
		
		if (input.isKeyJustPressed(Keys.RIGHT)) {
		  moveTicks += 1;
		}
		
		if (input.isKeyJustPressed(Keys.LEFT)) {
		  dt = 0;

		  timestamp -= 1;
		  if (timestamp < 0) timestamp = 0;
		  moveTicks = 0;
		}

		camera.update();
	}

	@Override
	public void draw() {
		renderer.begin();
		
		
		renderer.clear();

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
      
      for (int i = 0; i < droplets.size(); i++) {
        Droplet droplet = droplets.get(i);
        
        Point at = droplet.getPosition(timestamp);
        if (at == null) continue;

        Color color = null;
        if (droplet.operation == null) {
          color = Color.gray;
        } else {
          switch (droplet.operation.type) {
          case Merge:
            color = Color.orange;
            break;
          case Mix:
            color = Color.green;
            break;
          case Split:
            color = Color.CYAN;
            break;
          case Spawn:
            break;
          default:
            throw new IllegalStateException("broken!");
          }
        }
        
        Point move = new Point();
        Point next = droplet.getPosition(timestamp + 1);
        
        if (next == null) {
          if (droplet.operation == null) {
            renderer.setColor(color);
            
            float percentage = dt / (float) tilesize;
            
            float xx = (at.x + move.x * percentage) * tilesize + gap;
            float yy = (at.y + move.y * percentage) * tilesize + gap;
            
            float width = tilesize - gap * 2f;
            float height = tilesize - gap * 2f;
            
            renderer.fillOval(xx, yy, width, height);    
            
            String id = String.format("%d", i);
            
            float tx = xx + width / 2f;
            float ty = yy + height / 2f;
            
            renderer.setColor(Color.BLACK);
            renderer.drawText(id, tx, ty, Alignment.Center);
            
          } else {            
            Droplet[] successors = droplet.operation.forwarding;
            
            for (Droplet successor : successors) {
              renderer.setColor(color);
              
              next = successor.getPosition(timestamp + 1);
              move.set(next).sub(at);
              
              float percentage = dt / (float) tilesize;
              
              float xx = (at.x + move.x * percentage) * tilesize + gap;
              float yy = (at.y + move.y * percentage) * tilesize + gap;
              
              float width = tilesize - gap * 2f;
              float height = tilesize - gap * 2f;
              
              renderer.fillOval(xx, yy, width, height);    
              
              String id = String.format("%d", i);
              
              float tx = xx + width / 2f;
              float ty = yy + height / 2f;
              
              renderer.setColor(Color.BLACK);
              renderer.drawText(id, tx, ty, Alignment.Center);
            }
          }
        } else {
          renderer.setColor(color);
          
          move.set(next).sub(at);

          float percentage = dt / (float) tilesize;
          
          float xx = (at.x + move.x * percentage) * tilesize + gap;
          float yy = (at.y + move.y * percentage) * tilesize + gap;
          
          float width = tilesize - gap * 2f;
          float height = tilesize - gap * 2f;
          
          renderer.fillOval(xx, yy, width, height);    
          
          String id = String.format("%d", i);
          
          float tx = xx + width / 2f;
          float ty = yy + height / 2f;
          
          renderer.setColor(Color.BLACK);
          renderer.drawText(id, tx, ty, Alignment.Center);
          
        }
      }
    }
    
		renderer.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}
}
