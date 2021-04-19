package pack.algorithms;

public class MixingPercentages {
  
	public float forwardPercentage;
	public float turnPercentage;
	public float reversePercentage;
	public float firstPercentage;
	public float stationaryPercentage;
	
	public MixingPercentages() {
  }
	
	public float getMixingPercentage(Point move, Point previousMove) {
    if (previousMove == null) {
      boolean stay1 = (move.x == 0 && move.y == 0);
      if (stay1) return stationaryPercentage;
      return firstPercentage;
    } else {

      boolean stay1 = (move.x == 0 && move.y == 0);
      boolean left1 = (move.x == -1 && move.y == 0);
      boolean right1 = (move.x == 1 && move.y == 0);
      boolean up1 = (move.x == 0 && move.y == 1);
      boolean down1 = (move.x == 0 && move.y == -1);
      
      boolean stay0 = (previousMove.x == 0 && previousMove.y == 0);
      boolean left0 = (previousMove.x == -1 && previousMove.y == 0);
      boolean right0 = (previousMove.x == 1 && previousMove.y == 0);
      boolean up0 = (previousMove.x == 0 && previousMove.y == 1);
      boolean down0 = (previousMove.x == 0 && previousMove.y == -1);
      
      if (stay1) return stationaryPercentage;
      
      if (!stay1 && stay0) return firstPercentage;

      if (left1 && left0) return forwardPercentage;
      if (right1 && right0) return forwardPercentage;
      if (up1 && up0) return forwardPercentage;
      if (down1 && down0) return forwardPercentage;
      
      if (left1 && right0) return reversePercentage;
      if (right1 && left0) return reversePercentage;
      if (up1 && down0) return reversePercentage;
      if (down1 && up0) return reversePercentage;
      
      if ((up1 || down1) && (right0 || left0)) return turnPercentage;
      if ((left1 || right1) && (up0 || down0)) return turnPercentage;
      
      throw new IllegalStateException("broken! forgot mixing percentage.");
    }
  }
}
