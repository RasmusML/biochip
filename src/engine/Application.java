package engine;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;

import engine.input.InputHandler;
import engine.input.KeyboardHandler;
import engine.input.MouseHandler;

public class Application {

	private boolean debug = false;

	private boolean running;

	private int fps;
	private double msPerFrame;

	private int actualFps;
	private float actualMsPerFrame;

	private boolean resizable;

	private int initialWidth, initialHeight;
	private int width, height;

	private MouseHandler mouseHandler;
	private KeyboardHandler keyboardHandler;
	private InputHandler inputHandler;

	private Window window;
	private String title;

	private Thread appThread;

	private ApplicationAdapter appAdapter;
	
	
	public Application(ApplicationAdapter appAdapter, ApplicationConfiguration config) {
		this.appAdapter = appAdapter;
		
		// dual monitors crashes. For some reason, it can not create buffers for the canvas.
		// this fixes it.
		System.setProperty("sun.java2d.d3d", "false");

		title = config.title;
		resizable = config.resizable;

		initialWidth = config.width;
		initialHeight = config.height;

		width = initialWidth;
		height = initialHeight;

		int defaultFps = 60;
		fps = (config.fps <= 0) ? defaultFps : config.fps;
		msPerFrame = 1000 / (double) fps;

		window = new Window();

		initWindow();

		keyboardHandler = new KeyboardHandler();
		mouseHandler = new MouseHandler();
		inputHandler = new InputHandler(mouseHandler, keyboardHandler);

		appAdapter.input = inputHandler;
		appAdapter.app = this;
		
		appAdapter.init();
		appAdapter.resize(initialWidth, initialHeight);
		
		initialize();
	}

	private void initialize() {
		appThread = new Thread(new Runnable() {

			@Override
			public void run() {
				runMainLoop();
			}
		});

		appThread.start();
	}

	private void runMainLoop() {
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

			actualMsPerFrame = elapsed;

			keyboardHandler.act();
			mouseHandler.act();

			appAdapter.update();

			int currentWidth = window.getWidth();
			int currentHeight = window.getHeight();

			if (currentWidth != width || currentHeight != height) {
				appAdapter.resize(currentWidth, currentHeight);
				width = currentWidth;
				height = currentHeight;
			}

			appAdapter.draw();

			sync(elapsed);
		}
	}

	double accumulatedDelta;
	long theta = 3;

	private void sync(double elapsed) {
		accumulatedDelta += (elapsed - msPerFrame);

		long correctedMsPerFrame = (long) (msPerFrame - accumulatedDelta - theta);
		long msSleep = (correctedMsPerFrame < 0) ? 0 : correctedMsPerFrame; // @todo: yield when 0 ?
		Utils.sleep(msSleep);
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
		appAdapter.dispose();
		window.close();
	}

	public int getFps() {
		return actualFps;
	}

	public float getDelta() {
		return actualMsPerFrame;
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public void setRoot(Component component) {
		window.setRoot(component);
	}
	
	public void attachInputListenersToComponent(Component component) {
		mouseHandler.attach(component);
		keyboardHandler.attach(component);
	}
	
	public void setTitle(String newTitle) {
		window.setTitle(newTitle);
		title = newTitle;
	}
}
