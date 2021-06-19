package framework.scenes;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import framework.Application;
import framework.input.Input;

public class SceneManager {
	
	private Map<String, Scene> scenes;
	private Scene activeScene;
	
	private Application app;
	private Input input;
	
	public SceneManager(Application app, Input input) {
		this.app = app;
		this.input = input;
		
		scenes = new HashMap<>();
	}
	
	public void addScene(String name, Scene scene) {
		scene.app = app;
		scene.input = input;
		scene.manager = this;
		scene.init();
		
		scenes.put(name, scene);
	}
	
	public void changeScene(String name) {
		if (activeScene != null) activeScene.leave();
		Scene scene = scenes.get(name);
		scene.enter();
		activeScene = scene;
	}
	
	public void draw() {
		if (activeScene != null) activeScene.draw();
	}
	
	public void resize(int width, int height) {
		if (activeScene != null) activeScene.resize(width, height);
	}
	
	public void update() {
		if (activeScene != null) activeScene.update();
	}
	
	public void dispose() {
		for (Entry<String, Scene> entry : scenes.entrySet())   {
			Scene scene = entry.getValue();
			scene.dispose();
		}
	}
}
