package pack.algorithms.experimental;

public class Point {

  public int x, y;

  public Point() {
  }

  public Point(Point point) {
    this.x = point.x;
    this.y = point.y;
  }

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Point copy() {
    return new Point(x, y);
  }

  public Point add(Point p) {
    x += p.x;
    y += p.y;
    return this;
  }

  public Point sub(Point p) {
    x -= p.x;
    y -= p.y;
    return this;
  }

  public Point set(Point p) {
    x = p.x;
    y = p.y;
    return this;
  }

  @Override
  public String toString() {
    return String.format("(%d,%d)", x, y);
  }

}
