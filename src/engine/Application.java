package engine;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import engine.input.InputHandler;
import engine.input.KeyboardHandler;
import engine.input.MouseHandler;

public class Application {

	private boolean debug = false;

	private boolean running;

	private int fps;
	private int defaultFps;
	private double msPerFrame;

	private int actualFps;

	private boolean resizable;

	private int initialWidth, initialHeight;
	private int width, height;

	private MouseHandler mouseHandler;
	private KeyboardHandler keyboardHandler;
	private InputHandler inputHandler;

	private Window window;
	private String title;

	private Thread mainLoopThread;

	private ApplicationListener listener;
	
	private float delta;
	
	public Application(ApplicationListener listener, ApplicationConfiguration config) {
		this.listener = listener;
		
		// dual monitors crashes. For some reason, it can not create buffers for the canvas.
		// this fixes it.
		System.setProperty("sun.java2d.d3d", "false");

		title = config.title;
		resizable = config.resizable;

		initialWidth = config.width;
		initialHeight = config.height;

		width = initialWidth;
		height = initialHeight;

		defaultFps = 60;
		fps = (config.fps <= 0) ? defaultFps : config.fps;
		msPerFrame = 1000 / (double) fps;

		window = new Window();

		initWindow();

		keyboardHandler = new KeyboardHandler();
		mouseHandler = new MouseHandler();
		inputHandler = new InputHandler(mouseHandler, keyboardHandler);

		listener.input = inputHandler;
		listener.app = this;
		
		listener.init();
		listener.resize(initialWidth, initialHeight);
		
		initialize();
	}

	private void initialize() {
		mainLoopThread = new Thread(new Runnable() {

			@Override
			public void run() {
				startMainLoop();
			}
		});

		mainLoopThread.start();
	}

	private void startMainLoop() {
		running = true;

		int ticks = 0;
		long fpsAccumulatedTime = 0;
		long fpsResetTime = 1000;

		long previousTime = System.currentTimeMillis();

		while (running) {
			long now = System.currentTimeMillis();
			long elapsed = now - previousTime;
			previousTime = now;

			ticks++;
			fpsAccumulatedTime += elapsed;
			if (fpsAccumulatedTime >= fpsResetTime) {
				actualFps = (int) (ticks * 1_000 / fpsAccumulatedTime);
				ticks = 0;
				fpsAccumulatedTime = 0;
			}

			if (debug) {
				if (elapsed >= 2 * msPerFrame)
					System.out.printf("running behind %d\n", elapsed);
			}

			keyboardHandler.act();
			mouseHandler.act();

			delta = elapsed / 1_000f;
			listener.update();

			int currentWidth = window.getWidth();
			int currentHeight = window.getHeight();

			if (currentWidth != width || currentHeight != height) {
				listener.resize(currentWidth, currentHeight);
				width = currentWidth;
				height = currentHeight;
			}

			listener.draw();

			sync(elapsed);
		}
	}

	double accumulatedDelta;
	long theta = 3;

	private void sync(double elapsed) {
		accumulatedDelta += (elapsed - msPerFrame);

		long correctedMsPerFrame = (long) (msPerFrame - accumulatedDelta - theta);
		long msSleep = (correctedMsPerFrame < 0) ? 0 : correctedMsPerFrame; // @todo: yield when 0 ?
		sleep(msSleep);
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initWindow() {
		
		WindowAdapter onCloseListener = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				exit();
			}
		};
		
		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					window.init(title, initialWidth, initialHeight, resizable, onCloseListener);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void exit() {
		running = false;
		listener.dispose();
		window.close();
	}

	public int getFps() {
		return actualFps;
	}

	public float getDelta() {
		return delta;
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public void setRoot(Component component) {
		if (SwingUtilities.isEventDispatchThread()) {
			window.setRoot(component);
		} else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					window.setRoot(component);
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void attachComponentToListen(Component component) {
		mouseHandler.attach(component);
		keyboardHandler.attach(component);
	}
	
	public void setTitle(String newTitle) {
		window.setTitle(newTitle);
		title = newTitle;
	}
}
