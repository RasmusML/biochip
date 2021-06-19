package dmb.run;

import dmb.gui.App;
import framework.Application;
import framework.ApplicationConfiguration;

public class RunMeGUI {

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
