package engine.input;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KeyboardHandler {

	public boolean isPressed[];
	public boolean isJustPressed[];
	public boolean isJustReleased[];

	private Map<Integer, Integer> swingKeyCodesToKeyCodes;

	private ArrayList<KeyEvent> preKeys;
	private ArrayList<KeyEvent> pendingKeys;

	private KeyListener keyListener;
	
	private Component component;

	public KeyboardHandler() {

		swingKeyCodesToKeyCodes = new HashMap<>();

		isPressed = new boolean[Keys.numberOfKeys];
		isJustPressed = new boolean[Keys.numberOfKeys];
		isJustReleased = new boolean[Keys.numberOfKeys];
		
		registerAllKeys();

		pendingKeys = new ArrayList<>();
		preKeys = new ArrayList<>();

		keyListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				synchronized(preKeys) {
					preKeys.add(e);
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				synchronized(preKeys) {
					preKeys.add(e);
				}
			}
		};

	}
	
	public void attach(Component newComponent) {
		if (component != null) component.removeKeyListener(keyListener);
		newComponent.addKeyListener(keyListener);
		component = newComponent;
	}

	public void act() {
		reset();
		
		synchronized(preKeys) {
			pendingKeys.addAll(preKeys);
			preKeys.clear();
		}
		
		for (KeyEvent event : pendingKeys) {
			int swingKeyCode = event.getExtendedKeyCode();
			if (!swingKeyCodesToKeyCodes.containsKey(swingKeyCode)) {
				// System.out.printf("unsupported key %d\n", swingKeyCode);
				continue;
			}

			int keyCode = swingKeyCodesToKeyCodes.get(swingKeyCode);
			int eventId = event.getID();
			if (eventId == KeyEvent.KEY_PRESSED) {
				if (isPressed[keyCode]) {
					isJustPressed[keyCode] = false;
				} else {
					isJustPressed[keyCode] = true;
				}

				isPressed[keyCode] = true;

			} else if (eventId == KeyEvent.KEY_RELEASED) {
				if (isPressed[keyCode]) {
					isJustReleased[keyCode] = true;
					isPressed[keyCode] = false;
				}

			} else {
				throw new IllegalStateException("broken!");
			}
		}
		
		pendingKeys.clear();
	}

	public void reset() {
		for (int i = 0; i < Keys.numberOfKeys; i++) {
			isJustReleased[i] = false;
		}

		for (int i = 0; i < Keys.numberOfKeys; i++) {
			isJustPressed[i] = false;
		}
	}

	// @incomplete: register as necessary.
	private void registerAllKeys() {
		registerKey(KeyEvent.VK_A, Keys.A);
		registerKey(KeyEvent.VK_B, Keys.B);
		registerKey(KeyEvent.VK_C, Keys.C);
		registerKey(KeyEvent.VK_D, Keys.D);
		registerKey(KeyEvent.VK_E, Keys.E);
		registerKey(KeyEvent.VK_F, Keys.F);
		registerKey(KeyEvent.VK_G, Keys.G);
		registerKey(KeyEvent.VK_H, Keys.H);
		registerKey(KeyEvent.VK_I, Keys.I);
		registerKey(KeyEvent.VK_J, Keys.J);
		registerKey(KeyEvent.VK_K, Keys.K);
		registerKey(KeyEvent.VK_L, Keys.L);
		registerKey(KeyEvent.VK_M, Keys.M);
		registerKey(KeyEvent.VK_N, Keys.N);
		registerKey(KeyEvent.VK_O, Keys.O);
		registerKey(KeyEvent.VK_P, Keys.P);
		registerKey(KeyEvent.VK_Q, Keys.Q);
		registerKey(KeyEvent.VK_R, Keys.R);
		registerKey(KeyEvent.VK_S, Keys.S);
		registerKey(KeyEvent.VK_T, Keys.T);
		registerKey(KeyEvent.VK_U, Keys.U);
		registerKey(KeyEvent.VK_V, Keys.V);
		registerKey(KeyEvent.VK_W, Keys.W);
		registerKey(KeyEvent.VK_X, Keys.X);
		registerKey(KeyEvent.VK_Y, Keys.Y);
		registerKey(KeyEvent.VK_Z, Keys.Z);
		/*
		registerKey(16777414, Keys.�);
		registerKey(16777432, Keys.�);
		registerKey(16777413, Keys.�);
		*/
		registerKey(KeyEvent.VK_SHIFT, Keys.SHIFT);
		registerKey(KeyEvent.VK_SPACE, Keys.SPACE);
		registerKey(KeyEvent.VK_UP, Keys.UP);
		registerKey(KeyEvent.VK_DOWN, Keys.DOWN);
		registerKey(KeyEvent.VK_LEFT, Keys.LEFT);
		registerKey(KeyEvent.VK_RIGHT, Keys.RIGHT);
	}

	private void registerKey(int swingKeyCode, int keyCode) {
		swingKeyCodesToKeyCodes.put(swingKeyCode, keyCode);
	}

}

/*
 * public boolean isAnyPressed() { return isPressed(Button.LEFT) ||
 * isPressed(Button.MIDDLE) || isPressed(Button.RIGHT); }
 * 
 * public boolean isAnyClicked() { return isClicked(Button.LEFT) ||
 * isClicked(Button.MIDDLE) || isClicked(Button.RIGHT); }
 * 
 * public boolean isAnyReleased() { return isReleased(Button.LEFT) ||
 * isReleased(Button.MIDDLE) || isReleased(Button.RIGHT); }
 */
