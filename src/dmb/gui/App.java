package dmb.gui;

import java.awt.Image;

import dmb.helpers.IOUtils;
import framework.ApplicationAdapter;
import framework.scenes.SceneManager;

public class App extends ApplicationAdapter {

  private SceneManager manager;

  @Override
  public void init() {
    Image image = IOUtils.loadImage("/biochipIcon.png");
    app.setIconImage(image);

    manager = new SceneManager(app, input);

    Shared shared = new Shared();

    manager.addScene("selection", new SelectionScene(shared));
    manager.addScene("replay", new ReplayScene(shared));

    manager.changeScene("selection");
  }

  @Override
  public void update() {
    manager.update();
  }

  @Override
  public void draw() {
    manager.draw();
  }

  @Override
  public void dispose() {
    manager.dispose();
  }

  @Override
  public void resize(int width, int height) {
    manager.resize(width, height);
  }
}
