package dmb.algorithms;

import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.mixingpercentages.MixingPercentages;

public interface Router {
  public RoutingResult compute(BioAssay assay, BioArray array, MixingPercentages percentages);
}
