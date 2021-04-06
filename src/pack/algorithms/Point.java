package pack.algorithms;

public class Point {

	public int x, y;

	public Point() {
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

	public void set(Point p) {
		x = p.x;
		y = p.y;
	}
	
	@Override
	public String toString() {
		return String.format("(%d,%d)", x, y);
	}

}
