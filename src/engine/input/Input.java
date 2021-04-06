package engine.input;

public interface Input {
	
	public int getX();
	public int getY();
	
	public boolean isMouseJustPressed(int button);
	public boolean isMousePressed(int button);
	public boolean isMouseJustReleased(int button);
	
	public boolean isKeyJustPressed(int keyCode);
	public boolean isKeyPressed(int keyCode);
	public boolean isKeyJustReleased(int keyCode);
	public void releaseAllKeys();

}
