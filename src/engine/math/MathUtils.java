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
		return (int) Math.round(value + 0.499);
	}
	
	public static int floor(float value) {
		return (int) Math.round(value - 0.499);
	}
	
	public static int wrap(int value, int max) {
		int wrap = value % max;
		if (wrap < 0) wrap += max;
		return wrap;
	}
	
  public static int getManhattanDistance(int sx, int sy, int tx, int ty) {
    return getManhattanDistance(sx - tx, sy - ty);
  }
  
  public static int getManhattanDistance(int dx, int dy) {
    return Math.abs(dx) + Math.abs(dy);
  }
  
  public static float distance(float dx, float dy) {
    return (float) Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * @param min
   * @param max
   * @return random integer in the range [min;max] (both inclusive)
   */
  public static int randomInt(int min, int max) {
    return (int) (min + Math.random() * ((max + 1)- min));
  }  
}
