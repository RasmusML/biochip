package framework.graphics;

import framework.math.Vector2;

public class StretchedViewport implements Viewport {

	private float virtualWidth, virtualHeight;
	private float scaleX, scaleY;
	
	private int screenWidth;
	private int screenHeight;

	private Camera camera;
	
	private boolean flipped;

	public StretchedViewport(float virtualWidth, float virtualHeight) {
		this(virtualWidth, virtualHeight, false);
	}
	
	public StretchedViewport(float virtualWidth, float virtualHeight, boolean flipped) {
		this.virtualWidth = virtualWidth;
		this.virtualHeight = virtualHeight;
		this.flipped = flipped;

		tmp = new Vector2();
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	public void update(int screenWidth, int screenHeight) {
		this.screenHeight = screenHeight;
		this.screenWidth = screenWidth;
		
		scaleX = screenWidth / virtualWidth;
		scaleY = screenHeight / virtualHeight;
	}

	private final Vector2 tmp;

	public Vector2 worldToScreen(float x, float y) {
		float centerX = virtualWidth / 2.0f;
		float centerY = virtualHeight / 2.0f;
		
		float screenX = ((x - camera.x) * camera.zoom + centerX) * scaleX;
		float screenY = ((y - camera.y) * camera.zoom + centerY) * scaleY;
		
		if (flipped) screenY = screenHeight - screenY;
		
		return tmp.set(screenX, screenY);
	}

	public float getCameraZoom() {
		return camera.zoom;
	}

	public Vector2 screenToWorld(int x, int y) {
		float centerX = virtualWidth / 2.0f;
		float centerY = virtualHeight / 2.0f;

		float flippedScreenY = flipped ? screenHeight - y : y;
		float worldX = (x / scaleX - centerX) / camera.zoom + camera.x;
		float worldY = (flippedScreenY / scaleY - centerY) / camera.zoom + camera.y;

		return tmp.set(worldX, worldY);
	}

	@Override
	public float getScaleX() {
		return scaleX;
	}

	@Override
	public float getScaleY() {
		return scaleY;
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
