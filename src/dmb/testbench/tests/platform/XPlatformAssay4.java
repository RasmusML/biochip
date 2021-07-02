package dmb.testbench.tests.platform;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.InVitroBuilder;

public class XPlatformAssay4 extends BioAssay {
  
  public XPlatformAssay4() {
    build();
  }

  private void build() {
    InVitroBuilder builder = new InVitroBuilder();
    builder.createInVitro(1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
