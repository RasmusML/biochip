package dmb.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.BioArray;
import dmb.algorithms.BioAssay;
import dmb.algorithms.Droplet;
import dmb.algorithms.DropletSizeAwareGreedyRouter;
import dmb.algorithms.DropletUnit;
import dmb.algorithms.ElectrodeActivations;
import dmb.algorithms.Module;
import dmb.algorithms.Operation;
import dmb.algorithms.OperationType;
import dmb.algorithms.Point;
import dmb.algorithms.Router;
import dmb.algorithms.RoutingResult;
import dmb.algorithms.components.DefaultMixingPercentages;
import dmb.algorithms.components.ElectrodeActivationTranslator;
import dmb.algorithms.components.MixingPercentages;
import dmb.gui.timeline.CompactTimelineLayout;
import dmb.gui.timeline.SimpleTimelineLayout;
import dmb.gui.timeline.Timeline;
import dmb.gui.timeline.TimelineLayout;
import dmb.gui.timeline.TimelineUnit;
import dmb.helpers.Assert;
import dmb.helpers.IOUtils;
import dmb.testbench.tests.BlockingDispenserTestBioArray;
import dmb.testbench.tests.BlockingDispenserTestBioAssay;
import dmb.testbench.tests.CrowdedModuleBioArray;
import dmb.testbench.tests.CrowdedModuleBioAssay;
import dmb.testbench.tests.ModuleBioArray2;
import dmb.testbench.tests.ModuleBioAssay2;
import dmb.testbench.tests.PCRMixingTreeArray;
import dmb.testbench.tests.PCRMixingTreeAssay;
import dmb.testbench.tests.PlatformArray4;
import dmb.testbench.tests.PlatformAssay4;
import dmb.testbench.tests.Test3BioArray;
import dmb.testbench.tests.Test3BioAssay;
import dmb.testbench.tests.Test6BioArray;
import dmb.testbench.tests.Test6BioAssay;
import dmb.testbench.tests.functionality.DetectorArray1;
import dmb.testbench.tests.functionality.DetectorAssay1;
import dmb.testbench.tests.functionality.DisposeArray1;
import dmb.testbench.tests.functionality.DisposeAssay1;
import dmb.testbench.tests.functionality.MergeArray3;
import dmb.testbench.tests.functionality.MergeAssay3;
import engine.ApplicationAdapter;
import engine.graphics.Camera;
import engine.graphics.FitViewport;
import engine.graphics.Renderer;
import engine.input.Button;
import engine.input.Keys;
import engine.math.MathUtils;
import engine.math.Vector2;

public class App extends ApplicationAdapter {

	// graphics
	Renderer renderer;
	FitViewport viewport;
	Camera boardCamera;
	Camera timelineCamera;
  
	Canvas canvas;
	
	float maxZoom;
	float zoomScaler;
	
	Selected selected;
	
	boolean debug = false;

	// mouse
	float oldX, oldY;
	boolean dragging;

	boolean mouseWithinTimeline;

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
		stopTime = .25f; // 0.45f
		
		maxZoom = 4f;
		zoomScaler = 1.02f;

		canvas.createBufferStrategy(3);
		canvas.setIgnoreRepaint(true);

		viewport = new FitViewport(640, 480, true);
		boardCamera = new Camera();
		timelineCamera = new Camera();
		
		renderer = new Renderer(viewport);
		renderer.setCanvas(canvas);
		
		percentages = new DefaultMixingPercentages();

    assay = new DisposeAssay1();
    array = new DisposeArray1();
    
    assay = new BlockingDispenserTestBioAssay();
    array = new BlockingDispenserTestBioArray();

    assay = new Test3BioAssay();
    array = new Test3BioArray();
    
    assay = new CrowdedModuleBioAssay();
    array = new CrowdedModuleBioArray();

    assay = new PCRMixingTreeAssay();
    array = new PCRMixingTreeArray();

    assay = new ModuleBioAssay2();
    array = new ModuleBioArray2();

    assay = new PlatformAssay4();
    array = new PlatformArray4();

    assay = new MergeAssay3();
    array = new MergeArray3();
    
    assay = new DetectorAssay1();
    array = new DetectorArray1();
    
    assay = new Test6BioAssay();
    array = new Test6BioArray();
    
    selected = new Selected();
    
    timeline = new Timeline();
    timeline.minCursorHeight = 4;
    timeline.timescale = 1f;
    timeline.operationGap = 0.8f;
    timeline.operationHeight = 7;
    timeline.stretchScaler = 1.02f;
    timeline.bufferX = 25;
    timeline.offsetX = viewport.getVirtualWidth() / 5f;
    
    Router router = new DropletSizeAwareGreedyRouter();
		//router = new GreedyRouter();
		result = router.compute(assay, array, percentages);
		
		ElectrodeActivationTranslator translator = new ElectrodeActivationTranslator(array.width, array.height);
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
		
		{
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
		}
		
		if (!debug) {
		  timelineLayout = new CompactTimelineLayout();
  		//timelineLayout = new SimpleTimelineLayout();
  		timelineUnits = timelineLayout.pack(assay.getOperations());
  		
  		for (TimelineUnit unit : timelineUnits) {
  		  int end = unit.start + unit.duration;
  		  if (end > timeline.width) timeline.width = end;
  		  if (unit.y > timeline.height) timeline.height = unit.y;
  		}
  		
  		timeline.height += 1;
  
      float tx = (timestamp * timeline.timescale) + timeline.offsetX;
      float ty = viewport.getVirtualHeight() / 2f - 30;
      
      timelineCamera.lookAtNow(tx, ty);
		}
    
	}
	
	@Override
	public void update() {
		handleInput();
		
    float tx = (timestamp * timeline.timescale) + timeline.offsetX;
    float ty = viewport.getVirtualHeight() / 2f - 30;
    
    timelineCamera.lookAtNow(tx, ty);

    String title = String.format("@%d", app.getFps());
    app.setTitle(title);
		
		boardCamera.update();

		if (selected.unit != null) {
		  int end = selected.unit.start + selected.unit.duration;
		  if (timestamp >= end) {
		    selected.droplet = null;
		    selected.unit = null;
		  }
		}
		
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
    
    { // book-keeping
      viewport.setCamera(timelineCamera);
      Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
      
      float width = timeline.width * timeline.timescale;
      float height = timeline.height * (timeline.operationHeight + gap) - gap;
  
      mouseWithinTimeline = mouse.x >= -timeline.bufferX && mouse.x <= width + timeline.bufferX && mouse.y >= 0 && mouse.y <= height;
    }

		{ // timeline
  		viewport.setCamera(timelineCamera);
  		Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
  		
  		if (mouseWithinTimeline) {
  	    float suggestedTime = MathUtils.clamp(0, result.executionTime - 1, mouse.x / (float) timeline.timescale);
  	    timeline.suggestedTime = Math.round(suggestedTime);
  	    
  	    if (input.isMouseJustReleased(Button.LEFT)) {
  	      timestamp = timeline.suggestedTime;
  	      dt = 0;
  	      moving = false;
  	    }
  		}
		}
		
		{ // board
		  if (!mouseWithinTimeline) {
		    viewport.setCamera(boardCamera);
		    Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
		    
		    // selected droplet
		    if (input.isMouseJustReleased(Button.LEFT)) {
		      
		      List<Droplet> droplets = result.droplets;
		      outer: for (Droplet droplet : droplets) {
		        
		        for (DropletUnit unit : droplet.units) {
		          Point at = unit.route.getPosition(timestamp);
		          if (at == null) continue;
		          
		          float x = at.x * tilesize;
		          float y = at.y * tilesize;
              
		          float width = tilesize;
		          float height = tilesize;
		          
		          if (mouse.x >= x && mouse.x <= x + width && mouse.y >= y && mouse.y <= y + height) {
		            if (droplet == selected.droplet) {
		              // deselect
		              selected.droplet = null;
		              selected.unit = null;
		            } else {
		              // select
		              selected.droplet = droplet;

		              if (selected.droplet.operation != null) {
		                for (TimelineUnit timelineUnit : timelineUnits) {
		                  if (timelineUnit.operation == selected.droplet.operation) {
		                    selected.unit = timelineUnit;
		                  }
		                }
		              }
		            }
		            
		            break outer;
		          }
		        }
		      }
        }
		  }
		}
		
    { // global
      
      int stepSize = 10;
      if (input.isKeyJustPressed(Keys.K)) {
        timestamp = (int) MathUtils.clamp(0, result.executionTime - 1, timestamp - stepSize);
      }
      
      if (input.isKeyJustPressed(Keys.L)) {
        timestamp = (int) MathUtils.clamp(0, result.executionTime - 1, timestamp + stepSize);
      }
      
      if (input.isKeyJustPressed(Keys.E)) {
        timestamp = result.executionTime - 1;
      }

      if (input.isKeyPressed(Keys.W)) {
        timeline.timescale *= timeline.stretchScaler;
      }
      
      if (input.isKeyPressed(Keys.Q)) {
        timeline.timescale /= timeline.stretchScaler;
      }
      
      if (input.isKeyPressed(Keys.X)) {
        float zoom = boardCamera.targetZoom * zoomScaler;
        if (zoom > maxZoom) zoom = maxZoom;
        
        boardCamera.zoomNow(zoom);
      }
      
      if (input.isKeyPressed(Keys.Z)) {
        boardCamera.zoomNow(boardCamera.targetZoom / zoomScaler);
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
      
      if (input.isKeyJustPressed(Keys.RIGHT)) {
        running = false;
        step = false;
        timestamp += 1;
        
        if (timestamp > result.executionTime - 1) {
          timestamp = result.executionTime - 1;
        }
        
        dt = 0;
      }
      
      if (input.isKeyJustPressed(Keys.LEFT)) {
        running = false;
        step = false;
        dt = 0;

        timestamp -= 1;
        if (timestamp < 0) timestamp = 0;
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
    }
  }

	@Override
	public void draw() {
		renderer.begin();
		renderer.clear();

		drawBoard();
		if (!debug) drawTimeline();
    
		renderer.end();
	}

  private void drawTimeline() {
    viewport.setCamera(timelineCamera);
    
    {
      renderer.setColor(ColorPalette.backgroundGray);
      
      float xx = 0;
      float yy = 0;
      float width = timeline.width * timeline.timescale;
      float height = timeline.height * (timeline.operationHeight + gap) - gap;
      
      renderer.fillRect(xx, yy, width, height);
    }
    
    for (TimelineUnit unit : timelineUnits) {
      float width = unit.duration * timeline.timescale;
      float height = timeline.operationHeight;

      float x = unit.start * timeline.timescale;
      float y = unit.y * (height + gap);
      
      Color color = getOperationColor(unit.operation);
      renderer.setColor(color);

      renderer.fillRect(x, y, width, height);

      Color colorOutline = ColorPalette.black;
      renderer.setColor(colorOutline);
      renderer.drawRect(x, y, width, height);
    }
    
    int cursorHeight = Math.max(timeline.height, timeline.minCursorHeight);
    
    {
      float xx = timestamp * timeline.timescale;
      float yy = 0;
      float width = timeline.timescale;
      float height = cursorHeight * (timeline.operationHeight + gap) - gap;
      
      renderer.fillRect(xx, yy, width, height);
    }
    
    {
      if (mouseWithinTimeline) {
        renderer.setColor(ColorPalette.gray);
        
        float xx = timeline.suggestedTime * timeline.timescale;
        float yy = 0;
        float width = timeline.timescale;
        float height = cursorHeight * (timeline.operationHeight + gap) - gap;
        
        renderer.fillRect(xx, yy, width, height);
      }
    }
    
    {
      if (selected.unit != null) {
        
        float width = selected.unit.duration * timeline.timescale;
        float height = timeline.operationHeight;

        float x = selected.unit.start * timeline.timescale;
        float y = selected.unit.y * (height + gap);
        
        Color color = ColorPalette.timelineSelection;
        renderer.setColor(color);
        renderer.fillRect(x, y, width, height);
      }
    }
  }


  private void drawBoard() {
    viewport.setCamera(boardCamera);
    
    { // frame
			renderer.setColor(ColorPalette.gray);

			float xx = -gap;
			float yy = -gap;
			float width = array.width * tilesize + gap * 2f;
			float height = array.height * tilesize + gap * 2f;

			renderer.fillRect(xx, yy, width, height);
		}

		{ // grid tiles
		  renderer.setColor(ColorPalette.white);
		  
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
		    
		    Color color = getModuleColor(module);
		    renderer.setColor(color);
		    
        for (int x = 0; x < module.width; x++) {
          for (int y = 0; y < module.height; y++) {
            
            float xx = (module.position.x + x) * tilesize + gap;
            float yy = (module.position.y + y) * tilesize + gap;
            float width = tilesize - gap * 2f;
            float height = tilesize - gap * 2f;

            renderer.fillRect(xx, yy, width, height);

          }
        }
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
    
    { // selected droplets
      List<Droplet> dropletSelection = new ArrayList<>();
      if (selected.droplet != null) {
        if (selected.unit != null && selected.unit.operation.name.equals(OperationType.merge)) {
          Operation operation = selected.unit.operation;
          dropletSelection.add(operation.manipulating[0]);
          dropletSelection.add(operation.manipulating[1]);
        } else {
          dropletSelection.add(selected.droplet);
        }
      }
      
      for (Droplet droplet : dropletSelection) {
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
              
              float percentage = dt / (float) movementTime;
              
              float x = (at.x + move.x * percentage) * tilesize + gap;
              float y = (at.y + move.y * percentage) * tilesize + gap;
              float width = tilesize - gap * 2f;
              float height = tilesize - gap * 2f;

              renderer.setColor(ColorPalette.seeThroughGray);
              
              renderer.fillRect(x, y, width, height);
              
            } else {
              move.set(target).sub(at);
              
              float percentage = dt / (float) movementTime;
              
              float x = (at.x + move.x * percentage) * tilesize + gap;
              float y = (at.y + move.y * percentage) * tilesize + gap;
              float width = tilesize - gap * 2f;
              float height = tilesize - gap * 2f;

              renderer.setColor(ColorPalette.seeThroughGray);
              
              renderer.fillRect(x, y, width, height);
            }
          }
          
        } else {
          for (int i = 0; i < droplet.units.size(); i++) {
            DropletUnit dropletUnit = droplet.units.get(i);
            Point at = dropletUnit.route.getPosition(timestamp);
            if (at == null) continue;

            float x = at.x * tilesize + gap;
            float y = at.y * tilesize + gap;
            float width = tilesize - gap * 2f;
            float height = tilesize - gap * 2f;

            renderer.setColor(ColorPalette.seeThroughGray);
            
            renderer.fillRect(x, y, width, height);
          }
        }
      }
    }
  }

  private Color getModuleColor(Module module) {
    if (module.operation.equals(OperationType.dispense)) {
      return ColorPalette.seeThroughGray;
    } else if (module.operation.equals(OperationType.dispose)) {
      return ColorPalette.orange;
    } else if (module.operation.equals(OperationType.heating)) {
      return ColorPalette.seeThroughRed;
    } else if (module.operation.equals(OperationType.detection)) {
      return ColorPalette.magenta;
    }
    
    throw new IllegalStateException("unknown module operation");
  }

  private void drawDropletUnit(Droplet droplet, Point at, int dx, int dy) {
    float percentage = dt / (float) movementTime;
    
    float baseRadius = (float) Math.sqrt(1f / Math.PI);
    float baseDiameter = 2f * baseRadius;
    float diameterScaler = 1f / baseDiameter;

    float area = droplet.area / droplet.units.size();
    
    float unscaledRadius = (float) Math.sqrt(area / Math.PI); 
    float unscaledDiameter = 2f * unscaledRadius;
    
    float diameter = diameterScaler * unscaledDiameter;
    
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
    
    renderer.setColor(ColorPalette.black);
    //renderer.drawText(id, tx, ty, Alignment.Center);
    
    renderer.drawOval(x, y, width, height);
    //renderer.drawRect(x, y, width, height);
    
  }

  private Color getOperationColor(Operation operation) {
    if (operation == null) return ColorPalette.gray;
    
    switch (operation.name) {
    case OperationType.merge: 
      return ColorPalette.orange;
    case OperationType.mix:
      return ColorPalette.green;
    case OperationType.split:
      return ColorPalette.cyan;
    case OperationType.dispense:
      return ColorPalette.blue;
    case OperationType.heating:
      return ColorPalette.red;
    case OperationType.dispose:
      return ColorPalette.pink;
    case OperationType.detection:
      return ColorPalette.magenta;
    default:
      throw new IllegalStateException("broken!");
    }
  }

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}
}