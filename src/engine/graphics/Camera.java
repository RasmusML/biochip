package engine.graphics;

public class Camera {

	public float x, y;
	public float tx, ty;
	
	public float zoom;
	public float targetZoom;

	public float moveSpeed;
	public float zoomSpeed;

	public Camera() {
		moveSpeed = .08f;
		zoomSpeed = .06f;

		zoom = 1f;
		targetZoom = 1f;
	}

	public void lookAtNow(float x, float y) {
		this.x = x;
		this.y = y;
		this.tx = x;
		this.ty = y;
	}

	public void lookAt(float tx, float ty) {
		this.tx = tx;
		this.ty = ty;
	}

	public void update() {
		float dx = tx - x;
		float dy = ty - y;

		x += dx * moveSpeed;
		y += dy * moveSpeed;

		float dzoom = targetZoom - zoom;
		zoom += dzoom * zoomSpeed;
	}

	public void zoomNow(float zoom) {
		this.zoom = zoom;
		this.targetZoom = zoom;
	}

	public void zoom(float targetZoom) {
		this.targetZoom = targetZoom;
	}
	
}
