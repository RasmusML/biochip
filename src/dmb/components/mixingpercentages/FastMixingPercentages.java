package dmb.components.mixingpercentages;

public class FastMixingPercentages extends MixingPercentages {

  /**
   * Extreme forward mixing percentage to see droplet movement behavior-changes
   * during mixing.
   */

  public FastMixingPercentages() {
    forwardPercentage = 20.58f;
    reversePercentage = -0.5f;
    turnPercentage = 0.1f;
    firstPercentage = 0.29f;
    stationaryPercentage = 0f;
  }
}
