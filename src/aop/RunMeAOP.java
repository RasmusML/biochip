package aop;

import framework.Application;
import framework.ApplicationConfiguration;

public class RunMeAOP {

  public static void main(String[] args) {
    ApplicationConfiguration cfg = new ApplicationConfiguration();
    cfg.width = 640;
    cfg.height = 480;
    cfg.resizable = true;
    cfg.title = "AOP";
    cfg.fps = 60;

    new Application(new AopApp(), cfg);
  }

}
