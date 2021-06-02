package pack.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
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
import pack.algorithms.DropletAwareGreedyRouter;
import pack.algorithms.DropletUnit;
import pack.algorithms.ElectrodeActivations;
import pack.algorithms.Module;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;
import pack.algorithms.Point;
import pack.algorithms.Reservoir;
import pack.algorithms.Router;
import pack.algorithms.RoutingResult;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.ElectrodeActivationTranslator;
import pack.algorithms.components.MixingPercentages;
import pack.gui.timeline.CompactTimelineLayout;
import pack.gui.timeline.Timeline;
import pack.gui.timeline.TimelineLayout;
import pack.gui.timeline.TimelineUnit;
import pack.helpers.Assert;
import pack.helpers.IOUtils;
import pack.testbench.tests.BlockingDispenserTestBioArray;
import pack.testbench.tests.BlockingDispenserTestBioAssay;
import pack.testbench.tests.CrowdedModuleBioArray;
import pack.testbench.tests.CrowdedModuleBioAssay;
import pack.testbench.tests.ModuleBioArray4;
import pack.testbench.tests.ModuleBioAssay4;
import pack.testbench.tests.PCRMixingTreeArray;
import pack.testbench.tests.PCRMixingTreeAssay;
import pack.testbench.tests.PlatformArray4;
import pack.testbench.tests.PlatformAssay4;
import pack.testbench.tests.Test1BioArray;
import pack.testbench.tests.Test1BioAssay;
import pack.testbench.tests.functionality.DisposeArray1;
import pack.testbench.tests.functionality.DisposeAssay1;

public class App extends ApplicationAdapter {

	// graphics
	Renderer renderer;
	FitViewport viewport;
	Camera boardCamera;
	Camera timelineCamera;
	Canvas canvas;
	
	float maxZoom;

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
	TimelineLayout timelineLayout;
	List<TimelineUnit> timelineUnits;

	@Override
	public void init() {
	  Image image = IOUtils.loadImage("/biochipIcon.png");
	  app.setIconImage(image);
	  
		canvas = new Canvas();
		app.setRoot(canvas);
		app.attachInputListenersToComponent(canvas);
		
		movementTime = .12f;
		stopTime = .22f; // 0.45f
		
		maxZoom = 4f;

		canvas.createBufferStrategy(3);
		canvas.setIgnoreRepaint(true);

		viewport = new FitViewport(640, 480, true);
		boardCamera = new Camera();
		timelineCamera = new Camera();
		
		renderer = new Renderer(viewport);
		renderer.setCanvas(canvas);
		
		percentages = new DefaultMixingPercentages();

    assay = new PCRMixingTreeAssay();
    array = new PCRMixingTreeArray();

    assay = new DisposeAssay1();
    array = new DisposeArray1();
    
    assay = new Test1BioAssay();
    array = new Test1BioArray();

    assay = new BlockingDispenserTestBioAssay();
    array = new BlockingDispenserTestBioArray();
    
    assay = new CrowdedModuleBioAssay();
    array = new CrowdedModuleBioArray();

    assay = new ModuleBioAssay4();
    array = new ModuleBioArray4();

    assay = new PlatformAssay4();
    array = new PlatformArray4();
    
		Router router = new DropletAwareGreedyRouter();
		//Router router = new NotDropletAwareGreedyRouter();
		result = router.compute(assay, array, percentages);
		
		ElectrodeActivationTranslator translator = new ElectrodeActivationTranslator();
		ElectrodeActivations[] sections = translator.translateStateful(result.droplets, result.executionTime);

		/*

		for (int i = 0; i < sections.length; i++) {
		  ElectrodeActivationSection section = sections[i];
		  
		  System.out.printf("--- Section %d ---\n", i);
		  
		  for (ElectrodeActivation activation : section.activations) {
		    System.out.printf("%s:%s\n", activation.tile, activation.state);
		  }
		  
		  System.out.printf("\n");
		}
		*/
		
		float width = array.width * tilesize;
		float height = array.height * tilesize;
		
		float cx = width / 2f;
		float cy = height / 2f;
		boardCamera.lookAtNow(cx, cy);

		float zoomX = viewport.getVirtualWidth() / width;
		float zoomY = viewport.getVirtualHeight() / height;

		float zoom = Math.min(zoomX, zoomY);
		if (zoom > maxZoom) zoom = maxZoom;
		boardCamera.zoomNow(zoom);
		
		timelineCamera.lookAtNow(viewport.getVirtualWidth() / 2f - 100, viewport.getVirtualHeight() / 2f - 30);
		
		timeline = new Timeline();
		//timelineLayout = new SimpleTimelineLayout();
		timelineLayout = new CompactTimelineLayout();
		timelineUnits = timelineLayout.pack(assay.getOperations());
	}

	@Override
	public void update() {
		handleInput();

    String title = String.format("@%d", app.getFps());
    app.setTitle(title);
		
		boardCamera.update();
		
    if (timestamp <= result.executionTime - 2) {
      if (running) step = true;
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
    } else {
      moving = false;
    }
	}

  private void handleInput() {
    if (input.isKeyPressed(Keys.X)) {
		  float zoom = boardCamera.targetZoom * 1.02f;
		  if (zoom > maxZoom) zoom = maxZoom;
		  
			boardCamera.zoomNow(zoom);
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
  		
  		float suggestedTime = MathUtils.clamp(0, result.executionTime - 1, mouse.x / (float) timeline.timescale);
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
		  
		  if (timestamp > result.executionTime - 1) {
		    timestamp = result.executionTime - 1;
		  }
		}
		
		if (input.isKeyJustPressed(Keys.LEFT)) {
		  running = false;
		  step = false;
		  dt = 0;

		  timestamp -= 1;
		  if (timestamp < 0) timestamp = 0;
		}
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
    
    timeline.minCursorHeight = 4;
    timeline.timescale = 1f;
    timeline.operationGap = 0.8f;
    timeline.operationHeight = 7;
    
    int maxY = 0;
    
    for (TimelineUnit unit : timelineUnits) {
      if (unit.y > maxY) maxY = unit.y;
      
      float width = unit.duration * timeline.timescale;
      float height = timeline.operationHeight;

      float x = unit.start * timeline.timescale;
      float y = unit.y * (height + gap);
      
      Color color = getOperationColor(unit.operation);
      renderer.setColor(color);

      renderer.fillRect(x, y, width, height);

      Color colorOutline = Color.black;
      renderer.setColor(colorOutline);
      renderer.drawRect(x, y, width, height);
    }
    
    int cursorHeight = Math.max(maxY + 1, timeline.minCursorHeight);
    
    {
      float xx = timestamp * timeline.timescale;
      float yy = 0;
      float width = 1;
      float height = cursorHeight * (timeline.operationHeight + gap) - gap;
      
      renderer.fillRect(xx, yy, width, height);
    }
    
    {
      renderer.setColor(Color.gray);
      
      float xx = timeline.suggestedTime * timeline.timescale;
      float yy = 0;
      float width = 1;
      float height = cursorHeight * (timeline.operationHeight + gap) - gap;
      
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
        for (int x = 0; x < module.width; x++) {
          for (int y = 0; y < module.height; y++) {
            
            float xx = (module.position.x + x) * tilesize + gap;
            float yy = (module.position.y + y) * tilesize + gap;
            float width = tilesize - gap * 2f;
            float height = tilesize - gap * 2f;

            renderer.setColor(Color.white);
            renderer.fillRect(xx, yy, width, height);
            
            Color seeThroughRed = new Color(1f, 0f, 0f, 0.2f);
            renderer.setColor(seeThroughRed);
            renderer.fillRect(xx, yy, width, height);

          }
        }
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
        if (moving) {
          for (int i = 0; i < droplet.units.size(); i++) {
            DropletUnit dropletUnit = droplet.units.get(i);
            Point at = dropletUnit.route.getPosition(timestamp);
            if (at == null) continue;
    
            Point target = dropletUnit.route.getPosition(timestamp + 1);
            Point move = new Point();
            
            if (target == null) {
              Operation operation = droplet.operation;
              Assert.that(operation != null);
              
              // dispose operations don't have successor droplet units. So skip drawing those droplet units
              DropletUnit successor = dropletUnit.successor;
              if (successor == null) continue;
              Assert.that(!operation.name.equals(OperationType.dispose));
              
              target = successor.route.getPosition(timestamp + 1);
              move.set(target).sub(at);
              
              drawDropletUnit(droplet, at, move.x, move.y);
              
            } else {
              move.set(target).sub(at);
              
              drawDropletUnit(droplet, at, move.x, move.y);
            }
          }
          
        } else {
          for (int i = 0; i < droplet.units.size(); i++) {
            DropletUnit dropletUnit = droplet.units.get(i);
            Point at = dropletUnit.route.getPosition(timestamp);
            if (at == null) continue;

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
    //renderer.fillRect(x, y, width, height);    
    
    
    String id = String.format("%d", droplet.id);
    
    float tx = x + width / 2f;
    float ty = y + height / 2f;
    
    renderer.setColor(Color.BLACK);
    renderer.drawText(id, tx, ty, Alignment.Center);
    
    renderer.drawOval(x, y, width, height);
    //renderer.drawRect(x, y, width, height);
    
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
    case OperationType.dispose:
      return Color.pink;
    default:
      throw new IllegalStateException("broken!");
    }
  }

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}
}
