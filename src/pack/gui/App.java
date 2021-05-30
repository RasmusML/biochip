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
import pack.algorithms.DropletUnit;
import pack.algorithms.GreedyRouter;
import pack.algorithms.Module;
import pack.algorithms.NotDropletAwareGreedyRouter;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;
import pack.algorithms.Point;
import pack.algorithms.Reservoir;
import pack.algorithms.Router;
import pack.algorithms.RoutingResult;
import pack.algorithms.TrafficRouter;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.MixingPercentages;
import pack.tests.CrowdedModuleBioArray;
import pack.tests.CrowdedModuleBioAssay;
import pack.tests.PCRMixingTreeArray;
import pack.tests.PCRMixingTreeAssay;
import pack.tests.Test1BioArray;
import pack.tests.Test1BioAssay;
import pack.tests.functionality.MergeArray2;
import pack.tests.functionality.MergeAssay2;
import pack.tests.functionality.MixArray2;
import pack.tests.functionality.MixAssay2;

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
		
    assay = new MergeAssay2();
    array = new MergeArray2();
    
    assay = new MixAssay2();
    array = new MixArray2();
    
    assay = new Test1BioAssay();
    array = new Test1BioArray();

    assay = new CrowdedModuleBioAssay();
    array = new CrowdedModuleBioArray();
    
    assay = new PCRMixingTreeAssay();
    array = new PCRMixingTreeArray();
    
		percentages = new DefaultMixingPercentages();

		assay = new Test1BioAssay();
		array = new Test1BioArray();
		
		
		Router router = new NotDropletAwareGreedyRouter();
		//Router router = new NotDropletAwareGreedyRouter();
		result = router.compute(assay, array, percentages);
		
		/*
		ElectrodeActivationTranslator translator = new ElectrodeActivationTranslator();
		ElectrodeActivationSection[] sections = translator.translateStateful(result.droplets, result.executionTime);

		for (int i = 0; i < sections.length; i++) {
		  ElectrodeActivationSection section = sections[i];
		  
		  System.out.printf("--- Section %d ---\n", i);
		  
		  for (ElectrodeActivation activation : section.activations) {
		    System.out.printf("%s:%s\n", activation.tile, activation.state);
		  }
		  
		  System.out.printf("\n");
		}
		*/
		
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

		drawBoard();
    drawTimeline();
    
		renderer.end();
	}

  private void drawTimeline() {
    viewport.setCamera(timelineCamera);
    
    timeline.timescale = 1f;
    timeline.operationGap = 0.8f;
    timeline.operationHeight = 7;
    
    int nonDispenseOperation = 0;
    
    List<Operation> operations = assay.getOperations();
    for (int i = 0; i < operations.size(); i++) {
      Operation operation = operations.get(i);
      
      int start, end;
      if (operation.name.equals(OperationType.mix)) {
        Droplet droplet = operation.manipulating[0];
        
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else if (operation.name.equals(OperationType.merge)) {
        Droplet droplet0 = operation.manipulating[0];
        Droplet droplet1 = operation.manipulating[1];
        
        start = Math.max(droplet0.getStartTimestamp(), droplet1.getStartTimestamp());
        end = Math.min(droplet0.getEndTimestamp(), droplet1.getEndTimestamp());
      } else if (operation.name.equals(OperationType.split)) {
        Droplet droplet = operation.manipulating[0];
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else if (operation.name.equals(OperationType.dispense)){
        continue;
      } else if (operation.name.equals(OperationType.heating)) {
        Droplet droplet = operation.manipulating[0];
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else {
        throw new IllegalStateException("broken! " + operation.name);
      }
      
      nonDispenseOperation += 1;
      
      float width = (end - start) * timeline.timescale;
      float height = timeline.operationHeight;

      float x = start * timeline.timescale;
      float y = (height + gap) * i;
      
      Color color = getOperationColor(operation);
      renderer.setColor(color);

      renderer.fillRect(x, y, width, height);

      Color colorOutline = Color.black;
      renderer.setColor(colorOutline);
      renderer.drawRect(x, y, width, height);
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
		
		{ // module
      
      for (Module module : result.modules) {
        float x = module.position.x * tilesize;
        float y = module.position.y * tilesize;
        float width = module.width * tilesize;
        float height = module.height * tilesize;
        
        renderer.setColor(Color.black);
        renderer.fillRect(x, y, width, height);
      }
      
      for (Module module : result.modules) {
        float x = module.position.x * tilesize + gap;
        float y = module.position.y * tilesize + gap;
        float width = module.width * tilesize - 2 * gap;
        float height = module.height * tilesize - 2 * gap;
        
        renderer.setColor(Color.white);
        renderer.fillRect(x, y, width, height);
        
        Color seeThroughRed = new Color(1f, 0f, 0f, 0.2f);
        renderer.setColor(seeThroughRed);
        renderer.fillRect(x, y, width, height);
      }
    }
		
		{ // reservoirs
		  for (Reservoir reservoir : result.reservoirs) {
		    float xx = reservoir.position.x * tilesize + gap;
        float yy = reservoir.position.y * tilesize + gap;
        
        float width = tilesize - gap * 2f;
        float height = tilesize - gap * 2f;
        
        renderer.setColor(new Color(.1f, .2f, .3f, .4f));
        renderer.fillRect(xx, yy, width, height);
		  }
		}

    { // droplets
      
      List<Droplet> droplets = result.droplets;
      for (Droplet droplet : droplets) {
        for (int i = 0; i < droplet.units.size(); i++) {
          DropletUnit dropletUnit = droplet.units.get(i);
          Point at = dropletUnit.route.getPosition(timestamp);
          if (at == null) continue;
  
          Point target = dropletUnit.route.getPosition(timestamp + 1);
          Point move = new Point();
          
          if (target != null) move.set(target).sub(at);
          
          if (moving) {
            if (target == null) {
              if (droplet.operation == null) {
                drawDropletUnit(droplet, at, move.x, move.y);
              } else {            
                /*
                Droplet[] successors = droplet.operation.forwarding;

                for (Droplet successor : successors) {
                  DropletUnit successorUnit = successor.units.get(i);
                  target = successorUnit.route.getPosition(timestamp + 1);
                  move.set(target).sub(at);
                   
                  drawDropletUnit(droplet, at, move.x, move.y);
                }
                */
                
                drawDropletUnit(droplet, at, 0, 0);
              }
            } else {
              drawDropletUnit(droplet, at, move.x, move.y);
            }
          } else {
            drawDropletUnit(droplet, at, 0, 0);
          }
        }
      }
    }
  }

  private void drawDropletUnit(Droplet droplet, Point at, int dx, int dy) {
    float percentage = dt / (float) movementTime;
    
    float baseRadius = (float) Math.sqrt(1f / Math.PI);
    float baseDiameter = 2f * baseRadius;
    float diameterScaler = 1f / baseDiameter;

    float unscaledRadius = (float) Math.sqrt(droplet.area / Math.PI); 
    float unscaledDiameter = 2f * unscaledRadius;
    
    float diameter = diameterScaler * unscaledDiameter;
    
    diameter = 1; // @TODO: remove
    
    float size = tilesize * diameter;
    
    float offset = (tilesize - size) / 2f;
    
    float x = (at.x + dx * percentage) * tilesize + gap + offset;
    float y = (at.y + dy * percentage) * tilesize + gap + offset;
    
    float width = size - gap * 2f;
    float height = size - gap * 2f;
    
    Color color = getOperationColor(droplet.operation);
    renderer.setColor(color);
    
    renderer.fillOval(x, y, width, height);    
    
    String id = String.format("%d", droplet.id);
    
    float tx = x + width / 2f;
    float ty = y + height / 2f;
    
    renderer.setColor(Color.BLACK);
    renderer.drawText(id, tx, ty, Alignment.Center);
    
    renderer.drawOval(x, y, width, height);
  }

  private Color getOperationColor(Operation operation) {
    if (operation == null) return Color.gray;
    
    switch (operation.name) {
    case OperationType.merge: 
      return Color.orange;
    case OperationType.mix:
      return Color.green;
    case OperationType.split:
      return Color.cyan;
    case OperationType.dispense:
      return Color.blue;
    case OperationType.heating:
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
