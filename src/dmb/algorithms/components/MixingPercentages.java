package dmb.algorithms.components;

import dmb.algorithms.Move;

public class MixingPercentages {
  
	public float forwardPercentage;
	public float turnPercentage;
	public float reversePercentage;
	public float firstPercentage;
	public float stationaryPercentage;
	
	public MixingPercentages() {
  }
	
	public float getMixingPercentage(Move move, Move previousMove) {
    if (previousMove == null) {
      if (move == Move.None) return stationaryPercentage;
      return firstPercentage;
    } else {
      if (move == Move.None) return stationaryPercentage;

      if (move != Move.None && previousMove == Move.None) return firstPercentage;

      if (move == Move.Left && previousMove == Move.Left) return forwardPercentage;
      if (move == Move.Right && previousMove == Move.Right) return forwardPercentage;
      if (move == Move.Up && previousMove == Move.Up) return forwardPercentage;
      if (move == Move.Down && previousMove == Move.Down) return forwardPercentage;
      
      if (move == Move.Left && previousMove == Move.Right) return reversePercentage;
      if (move == Move.Right && previousMove == Move.Left) return reversePercentage;
      if (move == Move.Up && previousMove == Move.Down) return reversePercentage;
      if (move == Move.Down && previousMove == Move.Up) return reversePercentage;
      
      if ((move == Move.Up || move == Move.Down) && (previousMove == Move.Right || previousMove == Move.Left)) return turnPercentage;
      if ((move == Move.Left || move == Move.Right) && (previousMove == Move.Up || previousMove == Move.Down)) return turnPercentage;
      
      throw new IllegalStateException("broken! forgot mixing percentage.");
    }
  }
}
