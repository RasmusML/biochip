package dmb.algorithms;

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
  
  public Point add(int dx, int dy) {
    x += dx;
    y += dy;
    return this;
  }

  public Point sub(Point p) {
    x -= p.x;
    y -= p.y;
    return this;
  }
  
  public Point set(int nx, int ny) {
    x = nx;
    y = ny;
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + x;
    result = prime * result + y;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Point other = (Point) obj;
    if (x != other.x)
      return false;
    if (y != other.y)
      return false;
    return true;
  }
}
