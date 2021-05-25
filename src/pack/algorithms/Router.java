package pack.algorithms;

import pack.algorithms.components.MixingPercentages;

public interface Router {
  public RoutingResult compute(BioAssay assay, BioArray array, MixingPercentages percentages);
}
