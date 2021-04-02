package pack;

import engine.Application;
import engine.ApplicationConfiguration;

public class RunMe {

	public static void main(String[] args) {
		ApplicationConfiguration cfg = new ApplicationConfiguration();
		cfg.width = 640;
		cfg.height = 480;
		cfg.resizable = true;
		cfg.title = "biochip";
		cfg.fps = 60;
		
		new Application(new App(), cfg);
	}
}
