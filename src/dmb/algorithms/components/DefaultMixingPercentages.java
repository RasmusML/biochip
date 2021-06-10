package dmb.algorithms.components;

public class DefaultMixingPercentages extends MixingPercentages {

  public DefaultMixingPercentages() {
    forwardPercentage = 0.58f;
    reversePercentage = -0.5f;
    turnPercentage = 0.1f;
    firstPercentage = 0.29f;
    stationaryPercentage = 0f;
  }
}
