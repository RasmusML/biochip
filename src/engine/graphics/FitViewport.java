package engine.graphics;

import engine.math.Vector2;

public class FitViewport implements Viewport {

	private float virtualWidth, virtualHeight;
	private float scale;
	
	private int screenHeight;
	private int screenWidth;

	private Camera camera;
	
	private boolean flipped;
	
	public FitViewport(float virtualWidth, float virtualHeight) {
		this(virtualWidth, virtualHeight, false);
	}

	public FitViewport(float virtualWidth, float virtualHeight, boolean flip) {
		this.virtualWidth = virtualWidth;
		this.virtualHeight = virtualHeight;

		flipped = flip;
		
		tmp = new Vector2();
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	public void update(int screenWidth, int screenHeight) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		
		float ratioX = screenWidth / virtualWidth;
		float ratioY = screenHeight / virtualHeight;
		float ratio = Math.min(ratioX, ratioY);
		this.scale = ratio;
	}

	private final Vector2 tmp;

	public Vector2 worldToScreen(float x, float y) {
		float centerX = virtualWidth / 2.0f;
		float centerY = virtualHeight / 2.0f;
		
		float screenX = ((x - camera.x) * camera.zoom + centerX) * scale;
		float screenY = ((y - camera.y) * camera.zoom + centerY) * scale;
		
		if (flipped) screenY = screenHeight - screenY;
		
		return tmp.set(screenX, screenY);
	}

	public float getCameraZoom() {
		return camera.zoom;
	}

	public Vector2 screenToWorld(int x, int y) {
		float centerX = virtualWidth / 2.0f;
		float centerY = virtualHeight / 2.0f;

		float worldX = (x / scale - centerX) / camera.zoom + camera.x;
		float worldY = (y / scale - centerY) / camera.zoom + camera.y;

		return tmp.set(worldX, worldY);
	}

	@Override
	public float getScaleX() {
		return scale;
	}

	@Override
	public float getScaleY() {
		return scale;
	}
	
	@Override
	public boolean flipped() {
		return flipped;
	}

	@Override
	public float getVirtualWidth() {
		return virtualWidth;
	}

	@Override
	public float getVirtualHeight() {
		return virtualHeight;
	}

	@Override
	public int getScreenWidth() {
		return screenWidth;
	}

	@Override
	public int getScreenHeight() {
		return screenHeight;
	}
}
