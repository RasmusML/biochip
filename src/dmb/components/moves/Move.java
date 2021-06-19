package dmb.components.moves;

public enum Move {
  
  Up(0,1),
  Down(0,-1),
  Left(-1, 0),
  Right(1, 0),
  None(0, 0);
  
  public final int x, y;
  
  private Move(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  public static Move get(int dx, int dy) {
    if (dx == 0 && dy == 0) return None;
    else if (dx == 1 && dy == 0) return Right;
    else if (dx == -1 && dy == 0) return Left;
    else if (dx == 0 && dy == 1) return Up;
    else if (dx == 0 && dy == -1) return Down;
    throw new IllegalStateException("invalid move!");
  }
}
