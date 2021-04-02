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
}
