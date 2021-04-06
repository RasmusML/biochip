package engine.input;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

public class MouseHandler {

	public boolean isPressed[];
	public boolean isJustPressed[];
	public boolean isJustReleased[];

	public int x, y;
	
	private int numberOfButtons = 3;

	private MouseListener mouseListener;
	private MouseMotionListener mouseMotionListener;
	
	private ArrayList<MouseEvent> preMouses;
	private ArrayList<MouseEvent> pendingMouses;
	
	private Component component;
	
	public MouseHandler() {
		isPressed = new boolean[numberOfButtons];
		isJustPressed = new boolean[numberOfButtons];
		isJustReleased = new boolean[numberOfButtons];

		preMouses = new ArrayList<>();
		pendingMouses = new ArrayList<>();
		
		mouseListener = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				synchronized(preMouses) {
					preMouses.add(e);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				synchronized(preMouses) {
					preMouses.add(e);
				}
			}
		};
		
		mouseMotionListener = new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				synchronized(preMouses) {
					preMouses.add(e);
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				synchronized(preMouses) {
					preMouses.add(e);
				}
			}
		};
		
		// TODO:
		/*
		mouseWheelListener = new MouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				e.getWheelRotation();
			}
		};
		*/
	}
	
	public void attach(Component newComponent) {
		if (component != null) {
			component.removeMouseListener(mouseListener);
			component.removeMouseMotionListener(mouseMotionListener);
		}
		
		newComponent.addMouseListener(mouseListener);
		newComponent.addMouseMotionListener(mouseMotionListener);
		
		component = newComponent;
	}
	
	public void act() {
		reset();
		
		synchronized(preMouses) {
			pendingMouses.addAll(preMouses);
			preMouses.clear();
		}

		for (MouseEvent event : pendingMouses) {
			int eventId = event.getID();
			if (eventId == MouseEvent.MOUSE_PRESSED) {
				int button = getButtonType(event);
				if (!isPressed[button]) isJustPressed[button] = true;
				isPressed[button] = true;
			} else if (eventId == MouseEvent.MOUSE_RELEASED) {
				int button = getButtonType(event);
				isJustReleased[button] = true;
				isPressed[button] = false;
			} else if (eventId == MouseEvent.MOUSE_MOVED) {
				Point position = event.getPoint();
				x = position.x;
				y = position.y;
			} else if (eventId == MouseEvent.MOUSE_DRAGGED) {
				Point position = event.getPoint();
				x = position.x;
				y = position.y;
			} else {
				throw new IllegalStateException("we should not get other events!");
			}
		}
		
		pendingMouses.clear();
	}

	private int getButtonType(MouseEvent e) {

		if (e.getButton() == MouseEvent.BUTTON1) {
			return Button.LEFT;
		} else if (e.getButton() == MouseEvent.BUTTON2) { // @test
			return Button.MIDDLE;
		} else if (e.getButton() == MouseEvent.BUTTON3) {
			return Button.RIGHT;
		}

		// throw new IllegalStateException("not supported: " + e.getButton());'
		return Button.UNKNOWN;
		// Button is non of the above, when mouse moved or if
		// a mouse has extra/special buttons.
	}

	public void reset() {
		for (int i = 0; i < numberOfButtons; i++) {
			isJustReleased[i] = false;
		}

		for (int i = 0; i < numberOfButtons; i++) {
			isJustPressed[i] = false;
		}
	}
}
