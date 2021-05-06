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
import pack.algorithms.Droplet;
import pack.algorithms.GreedyRouter;
import pack.algorithms.Module;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;
import pack.algorithms.Point;
import pack.algorithms.Reservoir;
import pack.algorithms.RoutingResult;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.MixingPercentages;
import pack.tests.BlockingDispenserTestBioArray;
import pack.tests.BlockingDispenserTestBioAssay;
import pack.tests.ModuleBioArray3;
import pack.tests.ModuleBioAssay3;
import pack.tests.PCRMixingTreeAssay;
import pack.tests.Test3BioArray;

public class App extends ApplicationAdapter {

	// graphics
	Renderer renderer;
	FitViewport viewport;
	Camera boardCamera;
	Camera timelineCamera;
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
	
	RoutingResult result;
	int timestamp;
	
	float dt;

	boolean running;
	boolean step;
	
	boolean moving;
	float stopTime;
	float movementTime;
	
	Timeline timeline;

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
		timelineCamera = new Camera();
		
		renderer = new Renderer(viewport);
		renderer.setCanvas(canvas);
		
		assay = new PCRMixingTreeAssay();
		array = new Test3BioArray();
		
		assay = new BlockingDispenserTestBioAssay();
    array = new BlockingDispenserTestBioArray();
		
    assay = new ModuleBioAssay3();
    array = new ModuleBioArray3();
    
		percentages = new DefaultMixingPercentages();
		
		GreedyRouter router = new GreedyRouter();
		result = router.compute(assay, array, percentages);

		float cx = array.width * tilesize / 2f;
		float cy = array.height * tilesize / 2f;
		boardCamera.lookAtNow(cx, cy);
		timelineCamera.lookAtNow(viewport.getVirtualWidth() / 2f - 100, viewport.getVirtualHeight() / 2f - 30);
		
		timeline = new Timeline();
	}

	@Override
	public void update() {
	  if (running) step = true;
	  
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
	  
		String title = String.format("@%d", app.getFps());
		app.setTitle(title);

		if (input.isKeyPressed(Keys.X)) {
			boardCamera.zoomNow(boardCamera.targetZoom * 1.02f);
		}
		
		if (input.isKeyPressed(Keys.Z)) {
      boardCamera.zoomNow(boardCamera.targetZoom * 0.98f);
    }
		
		if (input.isKeyJustPressed(Keys.SPACE)) {
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
		  
		}
		
		if (input.isKeyJustPressed(Keys.R)) {
		  running = false;
		  step = false;
		  timestamp = 0;
		  dt = 0;
		}

		{
      viewport.setCamera(timelineCamera);
      Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
  		
  		float suggestedTime = MathUtils.clamp(0, result.executionTime, mouse.x / (float) timeline.timescale);
  		timeline.suggestedTime = Math.round(suggestedTime);
		}
		
    if (input.isMouseJustReleased(Button.LEFT)) {
      timestamp = timeline.suggestedTime;
      dt = 0;
      moving = false;
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
		
    viewport.setCamera(boardCamera);

		drawBoard();
    
    viewport.setCamera(timelineCamera);

		
    {
      timeline.timescale = 1f;
      timeline.operationGap = 0.8f;
      timeline.operationHeight = 7;
      
      int nonDispenseOperation = 0;
      
      List<Operation> operations = assay.getOperations();
      for (int i = 0; i < operations.size(); i++) {
        Operation operation = operations.get(i);
        
        int start, end;
        if (operation.type == OperationType.Mix) {
          Droplet droplet = operation.manipulating[0];
          start = droplet.route.start;
          end = start + droplet.route.path.size();
        } else if (operation.type == OperationType.Merge) {
          Droplet droplet0 = operation.manipulating[0];
          Droplet droplet1 = operation.manipulating[1];
          
          int length = Math.min(droplet0.route.path.size(), droplet1.route.path.size());
          start = Math.max(droplet0.route.start, droplet1.route.start);
          end = start + length;
        } else if (operation.type == OperationType.Split) {
          Droplet droplet = operation.manipulating[0];
          start = droplet.route.start;
          end = start + droplet.route.path.size();
        } else if (operation.type == OperationType.Dispense){
          //Assert.that(false);
          continue;
        } else if (operation.type == OperationType.Module) {
          Droplet droplet = operation.manipulating[0];
          start = droplet.route.start;
          end = start + droplet.route.path.size();
        } else {
          throw new IllegalStateException("broken! " + operation.type);
        }
        
        nonDispenseOperation += 1;
        
        float width = (end - start) * timeline.timescale;
        float height = timeline.operationHeight;

        float xx = start * timeline.timescale;
        float yy = (height + gap) * i;
        
        Color color = getOperationColor(operation);
        renderer.setColor(color);

        renderer.fillRect(xx, yy, width, height);

        Color colorOutline = Color.black;
        renderer.setColor(colorOutline);
        renderer.drawRect(xx, yy, width, height);
        
      }
      
      {
        
        float xx = timestamp * timeline.timescale;
        float yy = 0;
        float width = 1;
        float height = nonDispenseOperation * (timeline.operationHeight + gap) - gap;
        
        renderer.fillRect(xx, yy, width, height);
        
      }
      
      {
        
        renderer.setColor(Color.gray);
        
        float xx = timeline.suggestedTime * timeline.timescale;
        float yy = 0;
        float width = 1;
        float height = nonDispenseOperation * (timeline.operationHeight + gap) - gap;
        
        renderer.fillRect(xx, yy, width, height);

      }
    }
    
		renderer.end();
	}

  private void drawBoard() {
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
		
		{ // module
      
      for (Module module : result.modules) {
        float xx = module.position.x * tilesize;
        float yy = module.position.y * tilesize;
        float width = module.width * tilesize;
        float height = module.height * tilesize;
        
        renderer.setColor(Color.black);
        renderer.fillRect(xx, yy, width, height);
      }
      
      for (Module module : result.modules) {
        float xx = module.position.x * tilesize + gap;
        float yy = module.position.y * tilesize + gap;
        float width = module.width * tilesize - 2 * gap;
        float height = module.height * tilesize - 2 * gap;
        
        renderer.setColor(Color.white);
        renderer.fillRect(xx, yy, width, height);
        
        Color seeThroughRed = new Color(1f, 0f, 0f, 0.2f);
        renderer.setColor(seeThroughRed);
        renderer.fillRect(xx, yy, width, height);
      }
    }
		
		{ // reserviors
		  for (Reservoir reservior : result.reservoirs) {
		    float xx = reservior.position.x * tilesize + gap;
        float yy = reservior.position.y * tilesize + gap;
        
        float width = tilesize - gap * 2f;
        float height = tilesize - gap * 2f;
        
        renderer.setColor(new Color(.1f, .2f, .3f, .4f));
        renderer.fillRect(xx, yy, width, height);
		  }
		}

    { // droplets
      
      List<Droplet> droplets = result.droplets;
      for (int i = 0; i < droplets.size(); i++) {
        Droplet droplet = droplets.get(i);
        
        Point at = droplet.route.getPosition(timestamp);
        if (at == null) continue;

        Color color = getOperationColor(droplet.operation);
        
        Point move = new Point();
        Point next = droplet.route.getPosition(timestamp + 1);
        
        if (moving) {
          if (next == null) {
            if (droplet.operation == null) {
              renderer.setColor(color);
              
              float percentage = dt / (float) movementTime;
              
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
              
              renderer.drawOval(xx, yy, width, height);
              
              
            } else {            
              Droplet[] successors = droplet.operation.forwarding;
              
              for (Droplet successor : successors) {
                renderer.setColor(color);
                
                next = successor.route.getPosition(timestamp + 1);
                move.set(next).sub(at);
                
                float percentage = dt / (float) movementTime;
                
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
                
                renderer.drawOval(xx, yy, width, height);
                
              }
            }
          } else {
            renderer.setColor(color);
            
            move.set(next).sub(at);
  
            float percentage = dt / (float) movementTime;
            
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
            
            renderer.drawOval(xx, yy, width, height);
            
            
          }
        } else {
          renderer.setColor(color);
          
          move.set(0, 0);

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
          
          renderer.drawOval(xx, yy, width, height);
        }
      }
    }
  }

  private Color getOperationColor(Operation operation) {
    if (operation == null) return Color.gray;
    
    switch (operation.type) {
    case Merge: 
      return Color.orange;
    case Mix:
      return Color.green;
    case Split:
      return Color.cyan;
    case Dispense:
      return Color.blue;
    case Module:
      return Color.red;
    default:
      throw new IllegalStateException("broken!");
    }
  }

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}
}
