package pack.run;

import engine.Application;
import engine.ApplicationConfiguration;
import pack.gui.App;

public class RunMeGUI {

	public static void main(String[] args) {
		ApplicationConfiguration cfg = new ApplicationConfiguration();
		cfg.width = 640;
		cfg.height = 480;
		cfg.resizable = true;
		cfg.title = "biochip";
		cfg.fps = 60;
		
		new Application(new App(), cfg);
		//new Application(new DropletReshapeApp(), cfg);
	}
}
