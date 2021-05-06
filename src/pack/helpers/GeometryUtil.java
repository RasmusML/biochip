package pack.helpers;

public class GeometryUtil {

  public static boolean inside(int x, int y, int width, int height) {
    return inside(x, y, 0, 0, width, height);
  }
  
  public static boolean inside(int px, int py, int x, int y, int width, int height) {
    return px <= x + width - 1 && px >= x && py <= y + height - 1 && py >= y;
  }
}
