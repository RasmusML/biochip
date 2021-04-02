package engine.graphics;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import engine.math.Vector2;

public class Renderer {

	private boolean drawing;

	private Canvas canvas;
	private BufferStrategy bufferStrategy;
	private Graphics2D graphics;

	private String activeFontName;

	private Viewport viewport;

	private Map<String, Font> fonts;

	public Renderer(Viewport viewport) {
		this.viewport = viewport;

		fonts = new HashMap<>();
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	public void begin() {
		if (canvas == null) {
			throw new IllegalStateException("no canvas!");
		}

		if (drawing) {
			throw new IllegalStateException("already drawing, call end() first");
		}

		bufferStrategy = canvas.getBufferStrategy();
		graphics = (Graphics2D) bufferStrategy.getDrawGraphics();
		
		drawing = true;
	}

	public void clear(Color color) {
		ensureDrawing();
		graphics.setColor(color);
		graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
	}
	
	public void clear() {
		clear(Color.white);
	}

	public void setColor(float r, float g, float b) {
		ensureDrawing();
		graphics.setColor(new Color(r, g, b));
	}

	public void setColor(Color color) {
		ensureDrawing();
		graphics.setColor(color);
	}

	public void drawText(String text, float x, float y) {
		ensureDrawing();

		Vector2 screen = viewport.worldToScreen(x, y);

		int wx = Math.round(screen.x);
		int wy = Math.round(screen.y);

		graphics.drawString(text, wx, wy);
	}

	public void drawRect(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
		int wwidth = Math.round(width * viewport.getScaleX() * zoom);
		int wheight = Math.round(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;
		
		graphics.drawRect(wx, wy, wwidth, wheight);
	}

	public void fillRect(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
		int wwidth = Math.round(width * viewport.getScaleX() * zoom);
		int wheight = Math.round(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;
		
		graphics.fillRect(wx, wy, wwidth, wheight);
	}
	
	public void drawOval(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
		int wwidth = Math.round(width * viewport.getScaleX() * zoom);
		int wheight = Math.round(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;

		graphics.drawOval(wx, wy, wwidth, wheight);
	}
	
	public void fillOval(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
		int wwidth = Math.round(width * viewport.getScaleX() * zoom);
		int wheight = Math.round(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;

		graphics.fillOval(wx, wy, wwidth, wheight);
	}
	
	public void drawCircle(float x, float y, float radius) {
		drawOval(x, y, radius * 2f, radius * 2f);
	}
	
	public void fillCircle(float x, float y, float radius) {
		fillOval(x, y, radius * 2f, radius * 2f);
	}

	public void setFont(String name, int style, int size) {
		ensureDrawing();
		
		// @todo: just use images to do this, so we can scale.
		String id = String.format("%s-%d-%d", name, style, size);
		if (id.equals(activeFontName)) return;
		
		Font font = null;
		Font maybe = fonts.get(id);
		if (maybe != null) {
			font = maybe;
		} else {
			font = new Font(name, style, size);
			fonts.put(id, font);
		}

		graphics.setFont(font);
		
		activeFontName = id;
	}

	public void drawImage(BufferedImage img, float dx, float dy, float dw, float dh, int sx, int sy, int sw, int sh, float opacity) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(dx, dy);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);

		float zoom = viewport.getCameraZoom();
		int wwidth = Math.round(dw * viewport.getScaleX() * zoom);
		int wheight = Math.round(dh * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;
		
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
		graphics.drawImage(img, wx, wy, wx + wwidth, wy + wheight, sx, sy, sx + sw, sy + sh, null);
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

	}

	public void drawImage(BufferedImage img, float x, float y, float width, float height) {
		drawImage(img, x, y, width, height, 0, 0, img.getWidth(), img.getHeight(), 1);
	}
	
	public void drawImage(BufferedImage img, float x, float y, float width, float height, float opacity) {
		drawImage(img, x, y, width, height, 0, 0, img.getWidth(), img.getHeight(), opacity);
	}

	public void end() {
		ensureDrawing();
		
		drawing = false;
		activeFontName = null;

		graphics.dispose();
		bufferStrategy.show();
	}

	private void ensureDrawing() {
		if (!drawing) {
			throw new IllegalStateException("not drawing, call begin() first");
		}
	}
}
