package engine.graphics;

import engine.math.Vector2;

public interface Viewport {
	public void setCamera(Camera camera);
	public void update(int screenWidth, int screenHeight);
	public Vector2 worldToScreen(float x, float y);
	public Vector2 screenToWorld(int x, int y);
	public float getCameraZoom();
	public float getScaleX();
	public float getScaleY();
	public boolean flipped();
	public float getVirtualWidth();
	public float getVirtualHeight();
	public int getScreenWidth();
	public int getScreenHeight();
	
}

