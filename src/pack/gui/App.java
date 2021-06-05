package pack.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import engine.ApplicationAdapter;
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
import pack.testbench.tests.Test3BioArray;
import pack.testbench.tests.Test3BioAssay;
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
	float zoomScaler;
	
	Droplet selectedDroplet;
	TimelineUnit selectedTimelineUnit;
	
	boolean mouseWithinTimeline;
	
	boolean debug = false;

	// mouse
	float oldX, oldY;
	boolean dragging;

	// rendering
	float tilesize = 32f;
	float gap = 1f;
	
	// timeline input help
	float bufferX = 25;
	float offsetX;

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
    

    assay = new ModuleBioAssay4();
    array = new ModuleBioArray4();


    assay = new Test3BioAssay();
    array = new Test3BioArray();
    
    assay = new CrowdedModuleBioAssay();
    array = new CrowdedModuleBioArray();

    assay = new PCRMixingTreeAssay();
    array = new PCRMixingTreeArray();

    assay = new PlatformAssay4();
    array = new PlatformArray4();

    selectedDroplet = null;
    
    Router router = new DropletAwareGreedyRouter();
		//router = new NotDropletAwareGreedyRouter();
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
		
		timeline = new Timeline();
		timeline.minCursorHeight = 4;
		timeline.timescale = 1f;
		timeline.operationGap = 0.8f;
		timeline.operationHeight = 7;
		timeline.stretchScaler = 1.02f;
		
		
		if (!debug) {
		  
  		//timelineLayout = new SimpleTimelineLayout();
  		timelineLayout = new CompactTimelineLayout();
  		timelineUnits = timelineLayout.pack(assay.getOperations());
  		
  		for (TimelineUnit unit : timelineUnits) {
  		  int end = unit.start + unit.duration;
  		  if (end > timeline.width) timeline.width = end;
  		  if (unit.y > timeline.height) timeline.height = unit.y;
  		}
  		
  		offsetX = viewport.getVirtualWidth() * 1f / 5f;
  		
  		timeline.height += 1;
  
      float tx = (timestamp * timeline.timescale) + offsetX;
      float ty = viewport.getVirtualHeight() / 2f - 30;
      
      timelineCamera.lookAtNow(tx, ty);
		}
    
	}
	
	@Override
	public void update() {
		handleInput();
		
    float tx = (timestamp * timeline.timescale) + offsetX;
    float ty = viewport.getVirtualHeight() / 2f - 30;
    
    timelineCamera.lookAtNow(tx, ty);

    String title = String.format("@%d", app.getFps());
    app.setTitle(title);
		
		boardCamera.update();

		if (selectedTimelineUnit != null) {
		  int end = selectedTimelineUnit.start + selectedTimelineUnit.duration;
		  if (timestamp >= end) {
		    selectedDroplet = null;
		    selectedTimelineUnit = null;
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
  
      mouseWithinTimeline = mouse.x >= -bufferX && mouse.x <= width + bufferX && mouse.y >= 0 && mouse.y <= height;
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
		            
		            if (droplet == selectedDroplet) {
		              // deselect
		              selectedDroplet = null;
		              selectedTimelineUnit = null;
		            } else {
		              // select
		              selectedDroplet = droplet;

		              if (selectedDroplet.operation != null) {
		                for (TimelineUnit timelineUnit : timelineUnits) {
		                  if (timelineUnit.operation == selectedDroplet.operation) {
		                    selectedTimelineUnit = timelineUnit;
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
      if (selectedTimelineUnit != null) {
        
        float width = selectedTimelineUnit.duration * timeline.timescale;
        float height = timeline.operationHeight;

        float x = selectedTimelineUnit.start * timeline.timescale;
        float y = selectedTimelineUnit.y * (height + gap);
        
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
		
		/*
		{ // reservoirs
		  for (Reservoir reservoir : result.reservoirs) {
		    float xx = reservoir.position.x * tilesize + gap;
        float yy = reservoir.position.y * tilesize + gap;
        
        float width = tilesize - gap * 2f;
        float height = tilesize - gap * 2f;
        
        renderer.setColor(ColorPalette.seeThroughGray);
        renderer.fillRect(xx, yy, width, height);
		  }
		}
		*/

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
      List<Droplet> selected = new ArrayList<>();
      if (selectedDroplet != null) {
        Operation operation = selectedTimelineUnit.operation;
        
        if (operation != null && operation.name.equals(OperationType.merge)) {
          selected.add(operation.manipulating[0]);
          selected.add(operation.manipulating[1]);
        } else {
          selected.add(selectedDroplet);
        }
      }
      
      for (Droplet droplet : selected) {
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
    default:
      throw new IllegalStateException("broken!");
    }
  }

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}
}
