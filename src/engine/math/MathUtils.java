package engine.math;

public class MathUtils {
	
	public static float clamp(float min, float max, float value) {
		if (value <= min) return min;
		else if (value >= max) return max;
		return value;
	}
	
	public static float lerp(float from, float to, float percentage) {
		return (to - from) * percentage + from;
	}

	public static int ceil(float value) {
	  return (int) ((value % 1 == 0) ? value : value + 1);
	}
	
	public static int floor(float value) {
		return (int) value;
	}
	
	public static int wrap(int value, int max) {
		int wrap = value % max;
		if (wrap < 0) wrap += max;
		return wrap;
	}
	
  public static float getManhattanDistance(float sx, float sy, float tx, float ty) {
    return getManhattanDistance(sx - tx, sy - ty);
  }
  
  public static float getManhattanDistance(float dx, float dy) {
    return Math.abs(dx) + Math.abs(dy);
  }
  
  public static float distance(float x1, float y1, float x2, float y2) {
    return distance(x1-x2, y1-y2);
  }
  
  public static float distance(float dx, float dy) {
    return (float) Math.sqrt(dx * dx + dy * dy);
  }
}
