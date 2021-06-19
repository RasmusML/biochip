package framework.scenes;

import framework.Application;
import framework.input.Input;

public class Scene {
	
	public Input input;
	public Application app;
	public SceneManager manager;
	
	public void init() {}
	public void enter() {}
	public void update() {}
	public void draw() {}
	public void leave() {}
	public void resize(int width, int height) {}
	public void dispose() {}
}
