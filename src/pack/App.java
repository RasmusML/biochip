package pack;

import java.awt.Canvas;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import engine.ApplicationListener;
import engine.graphics.Camera;
import engine.graphics.FitViewport;
import engine.graphics.Renderer;
import engine.input.Button;
import engine.math.Vector2;

public class App extends ApplicationListener {

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
	List<Droplet> droplets;

	@Override
	public void init() {
		canvas = new Canvas();
		app.setRoot(canvas);
		app.attachComponentToListen(canvas);

		canvas.createBufferStrategy(3);

		viewport = new FitViewport(2 * 640, 2 * 480);
		camera = new Camera();
		renderer = new Renderer(viewport);

		viewport.setCamera(camera);
		renderer.setCanvas(canvas);

		grid = new Grid();
		grid.width = 14;
		grid.height = 10;

		float cx = viewport.getVirtualWidth() / 2f - grid.width * (tilesize - 2 * gap);
		float cy = viewport.getVirtualHeight() / 2f - grid.height * (tilesize - 2 * gap);
		camera.lookAtNow(cx, cy);
		camera.zoom(2f);

		int dropletCount = 10;
		droplets = new ArrayList<>();

		for (int i = 0; i < dropletCount; i++) {
			Droplet droplet = new Droplet();
			droplet.path = new ArrayList<>();
			droplet.at = 0;

			droplets.add(droplet);
		}
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
			Vector2 mouse = viewport.screenToWorld(input.getX(), input.getY());
			oldX = mouse.x;
			oldY = mouse.y;

			dragging = true;
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

		renderer.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(canvas.getWidth(), canvas.getHeight());
	}

	static private class Grid {
		public int width, height;
	}

	static private class Droplet {
		public int at;
		public List<Point> path;
	}

	static private class Point {
		public int x, y;
	}
}
