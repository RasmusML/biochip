package engine.graphics;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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

	private Font activeFont;
	private FontRenderContext frc;

	private Viewport viewport;

	private Map<String, Font> fonts;

	public Renderer(Viewport viewport) {
		this.viewport = viewport;

		fonts = new HashMap<>();
		
		Font defaultFont = getFont("consolas", Font.PLAIN, 12);
		activeFont = defaultFont;
		
		AffineTransform transform = new AffineTransform();
		frc = new FontRenderContext(transform, true, true);
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

	public void drawText(String text, float x, float y, Alignment alignment) {
		ensureDrawing();

		float zoom = viewport.getCameraZoom();
		int scaledFontSize = (int) Math.round(activeFont.getSize() * viewport.getScaleY() * zoom);
		
		Font font = getFont(activeFont.getName(), activeFont.getStyle(), scaledFontSize);
    graphics.setFont(font);
    
    TextLayout layout = new TextLayout(text, font, frc);
    Rectangle2D bounds = layout.getBounds();
    
    float offsetX, offsetY;
    switch (alignment) {
    case Center:
      offsetX = (float) -bounds.getWidth() / 2f;
      offsetY = (float) bounds.getHeight() / 2f;
      break;
  	case BottomLeft:
      offsetX = 0;
      offsetY = 0;
      break;
    case TopLeft:
      offsetX = 0;
      offsetY = (float) bounds.getHeight();
      break;
    default: 
      throw new IllegalStateException("invalid alignment!");
    }

    Vector2 screen = viewport.worldToScreen(x, y);
    int wx = Math.round(screen.x + offsetX);
    int wy = Math.round(screen.y + offsetY);
		
		graphics.drawString(text, wx, wy);
	}
	
	 public void setFont(String name, int style, int size) {
	    Font font = getFont(name, style, size);
	    activeFont = font;
	  }
	  
  private Font getFont(String name, int style, int size) {
    Font font = null;
    String id = String.format("%s-%d-%d", name, style, size);
    Font maybe = fonts.get(id);
    if (maybe != null) {
      font = maybe;
    } else {
      font = new Font(name, style, size);
      fonts.put(id, font);
    }
    return font;
  }

	public void drawRect(float x, float y, float width, float height) {
		ensureDrawing();
		
		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
		int wwidth = (int) Math.floor(width * viewport.getScaleX() * zoom);
    int wheight = (int) Math.floor(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;
		
		graphics.drawRect(wx, wy, wwidth, wheight);
	}

	public void fillRect(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
		int wwidth = (int) Math.floor(width * viewport.getScaleX() * zoom);
		int wheight = (int) Math.floor(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;
		
		graphics.fillRect(wx, wy, wwidth, wheight);
	}
	
	public void drawOval(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
    int wwidth = (int) Math.floor(width * viewport.getScaleX() * zoom);
    int wheight = (int) Math.floor(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;

		graphics.drawOval(wx, wy, wwidth, wheight);
	}
	
	public void fillOval(float x, float y, float width, float height) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(x, y);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);
		
		float zoom = viewport.getCameraZoom();
    int wwidth = (int) Math.floor(width * viewport.getScaleX() * zoom);
    int wheight = (int) Math.floor(height * viewport.getScaleY() * zoom);
		
		if (viewport.flipped())	wy -= wheight;

		graphics.fillOval(wx, wy, wwidth, wheight);
	}
	
	public void drawCircle(float x, float y, float radius) {
		drawOval(x, y, radius * 2f, radius * 2f);
	}
	
	public void fillCircle(float x, float y, float radius) {
		fillOval(x, y, radius * 2f, radius * 2f);
	}

	public void drawImage(BufferedImage img, float dx, float dy, float dw, float dh, int sx, int sy, int sw, int sh, float opacity) {
		ensureDrawing();

		Vector2 position = viewport.worldToScreen(dx, dy);
		int wx = Math.round(position.x);
		int wy = Math.round(position.y);

		float zoom = viewport.getCameraZoom();
    int wwidth = (int) Math.floor(dw * viewport.getScaleX() * zoom);
    int wheight = (int) Math.floor(dh * viewport.getScaleY() * zoom);
		
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

		bufferStrategy.show();
		graphics.dispose();
	}

	private void ensureDrawing() {
		if (!drawing) {
			throw new IllegalStateException("not drawing, call begin() first");
		}
	}
}
