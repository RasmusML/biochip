package pack.run;

import engine.Application;
import engine.ApplicationConfiguration;
import pack.reshaping.DropletReshapeApp;

public class RunMeDropletReshaper {

  public static void main(String[] args) {
    ApplicationConfiguration cfg = new ApplicationConfiguration();
    cfg.width = 640;
    cfg.height = 480;
    cfg.resizable = true;
    cfg.title = "Reshaper";
    cfg.fps = 60;
    
    new Application(new DropletReshapeApp(), cfg);
  }
}
