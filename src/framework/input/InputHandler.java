package framework.input;

public class InputHandler implements Input {
	
	private MouseHandler mouseHandler;
	private KeyboardHandler keyboardHandler;
	
	public InputHandler(MouseHandler mouseHandler, KeyboardHandler keyboardHandler) {
		this.mouseHandler = mouseHandler;
		this.keyboardHandler = keyboardHandler;
	}

	@Override
	public int getX() {
		return mouseHandler.x;
	}

	@Override
	public int getY() {
		return mouseHandler.y;
	}

	@Override
	public boolean isMouseJustPressed(int button) {
		return mouseHandler.isJustPressed[button];
	}

	@Override
	public boolean isMousePressed(int button) {
		return mouseHandler.isPressed[button];
	}

	@Override
	public boolean isMouseJustReleased(int button) {
		return mouseHandler.isJustReleased[button];
	}

	@Override
	public boolean isKeyJustPressed(int keyCode) {
		return keyboardHandler.isJustPressed[keyCode];
	}

	@Override
	public boolean isKeyPressed(int keyCode) {
		return keyboardHandler.isPressed[keyCode];
	}

	@Override
	public boolean isKeyJustReleased(int keyCode) {
		return keyboardHandler.isJustReleased[keyCode];
	}
}
