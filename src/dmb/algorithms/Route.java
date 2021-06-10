package dmb.algorithms;

import java.util.ArrayList;
import java.util.List;

public class Route {
  
	public int start;
	public List<Point> path = new ArrayList<>();
	
  public Point getPosition(int timestamp) {
    int index = timestamp - start;
    if (index < 0 || index >= path.size()) return null;
    return path.get(index);
  }
  
  public Point getPosition() {
    if (path.size() == 0) return null;
    return path.get(path.size() - 1);
  }
  
  public Move getMove(int timestamp) {
    Point to = getPosition(timestamp + 1);
    if (to == null) return null;

    Point from = getPosition(timestamp);
    if (from == null) return null;
    
    int dx = to.x - from.x;
    int dy = to.y - from.y;
    
    return Move.get(dx, dy);
  }
}