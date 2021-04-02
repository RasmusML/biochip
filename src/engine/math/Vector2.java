package engine.math;

public class Vector2 {

	public float x, y;

	public Vector2 () {}

	public Vector2 (Vector2 v) {
		this.x = v.x;
		this.y = v.y;
	}

	public Vector2 (float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vector2 set (float x, float y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public Vector2 set (Vector2 v) {
		this.x = v.x;
		this.y = v.y;
		return this;
	}

	public Vector2 scl (float scalar) {
		this.x *= scalar;
		this.y *= scalar;
		return this;
	}

	public Vector2 normalize () {
		float len = length();
		if (len == 0) return this;
		x /= len;
		y /= len;
		return this;
	}

	public Vector2 sub (Vector2 v) {
		this.x -= v.x;
		this.y -= v.y;
		return this;
	}

	public Vector2 add (float x, float y) {
		this.x += x;
		this.y += y;
		return this;
	}

	public Vector2 sub (float x, float y) {
		this.x -= x;
		this.y -= y;
		return this;
	}

	public Vector2 add (Vector2 v) {
		this.x += v.x;
		this.y += v.y;
		return this;
	}

	public Vector2 copy () {
		return new Vector2(x, y);
	}

	public float lengthSquared () {
		return x * x + y * y;
	}

	public float distance (Vector2 position) {
		float dx = position.x - x;
		float dy = position.y - y;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public float length () {
		return (float) Math.sqrt(x * x + y * y);
	}

	public float distance2 (Vector2 vec) {
		float dx = vec.x - x;
		float dy = vec.y - y;
		return dx * dx + dy * dy;
	}
	
	public Vector2 rotate(float radians) {
		float cos = (float)Math.cos(radians);
		float sin = (float)Math.sin(radians);
		
		float newX = x * cos - y * sin;
		float newY = x * sin + y * cos;
		
		x = newX;
		y = newY;
		
		return this;
	}
	
	@Override
	public String toString() {
		return String.format("(%f, %f)", x, y);
	}
}
