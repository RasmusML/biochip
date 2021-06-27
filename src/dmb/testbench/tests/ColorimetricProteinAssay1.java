package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.ColorimetricProteinBuilder;

public class ColorimetricProteinAssay1 extends BioAssay {
  
  public ColorimetricProteinAssay1() {
    build();
  }

  private void build() {
    ColorimetricProteinBuilder builder = new ColorimetricProteinBuilder();
    builder.createColorimetricProtein(4, 8);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
