package pack.gui;

import java.awt.Canvas;
import java.awt.Color;
import java.util.List;

import engine.ApplicationAdapter;
import engine.graphics.Camera;
import engine.graphics.FitViewport;
import engine.graphics.Renderer;
import engine.input.Button;
import engine.input.Keys;
import engine.math.Vector2;
import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.DefaultMixingPercentages;
import pack.algorithms.MergeRouter;
import pack.algorithms.MixingPercentages;
import pack.algorithms.Point;
import pack.algorithms.Route;
import pack.algorithms.Test2BioArray;
import pack.algorithms.Test2BioAssay;

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

	// routing
	Grid grid;
	
	List<Route> routes;
	int timestamp;

	@Override
	public void init() {
		canvas = new Canvas();
		app.setRoot(canvas);
		app.attachInputListenersToComponent(canvas);

		canvas.createBufferStrategy(3);

		viewport = new FitViewport(2 * 640, 2 * 480);
		camera = new Camera();
		renderer = new Renderer(viewport);

		viewport.setCamera(camera);
		renderer.setCanvas(canvas);

		grid = new Grid();
		grid.width = 7;
		grid.height = 7;

		float cx = grid.width * tilesize / 2f;
		float cy = grid.height * tilesize / 2f;
		camera.lookAtNow(cx, cy);
		camera.zoomNow(2f);

		BioAssay assay = new Test2BioAssay();
		BioArray array = new Test2BioArray();
		MixingPercentages percentages = new DefaultMixingPercentages();
		
		MergeRouter router = new MergeRouter();
		routes = router.compute(assay, array, percentages);
		
		System.out.println(routes.size());
	}

	@Override
	public void update() {
		camera.update();

		String title = String.format("@%d", app.getFps());
		app.setTitle(title);

		if (input.isMousePressed(Button.RIGHT)) {
			camera.zoomNow(camera.targetZoom * 1.05f);
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
		  timestamp += 1;
		}
		
		if (input.isKeyJustPressed(Keys.LEFT)) {
		  timestamp -= 1;
		  if (timestamp < 0) timestamp = 0;
		}
	}

	@Override
	public void draw() {
		renderer.begin();
		renderer.clear();

		{ // frame
			renderer.setColor(Color.GRAY);

			float xx = -gap;
			float yy = -gap;
			float width = grid.width * tilesize + gap * 2f;
			float height = grid.height * tilesize + gap * 2f;

			renderer.fillRect(xx, yy, width, height);
		}

		{ // grid tiles
			renderer.setColor(Color.WHITE);
			for (int x = 0; x < grid.width; x++) {
				for (int y = 0; y < grid.height; y++) {

					float xx = x * tilesize + gap;
					float yy = y * tilesize + gap;

					float width = tilesize - gap * 2f;
					float height = tilesize - gap * 2f;

					renderer.fillRect(xx, yy, width, height);
				}
			}
		}

		/*
		{	// routes
			
			for (int i = 0; i < routes.size(); i++) {
				Route route = routes.get(i);
				
				float r = i / (float) routes.size();
				Color color = new Color(r, r, r);
				renderer.setColor(color);
				
				for (Point tile : route.path) {
					float xx = tile.x * tilesize + gap;
					float yy = tile.y * tilesize + gap;

					float width = tilesize - gap * 2f;
					float height = tilesize - gap * 2f;
					
					renderer.fillRect(xx, yy, width, height);
				}
			}
		}
		*/
		
    { // routes
      
      for (int i = 0; i < routes.size(); i++) {
        Route route = routes.get(i);
        
        float r = i / (float) routes.size();
        Color color = new Color(r, r, r);
        renderer.setColor(color);
        
        int pathIndex = timestamp - route.start;
        
        if (pathIndex < 0 || pathIndex >= route.path.size()) continue;
        
        Point tile = route.path.get(pathIndex);
        float xx = tile.x * tilesize + gap;
        float yy = tile.y * tilesize + gap;

        float width = tilesize - gap * 2f;
        float height = tilesize - gap * 2f;
        
        renderer.fillOval(xx, yy, width, height);        
      }
    }

		renderer.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}

	static private class Grid {
		public int width, height;
	}

}
