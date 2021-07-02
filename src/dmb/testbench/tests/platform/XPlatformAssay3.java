package dmb.testbench.tests.platform;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class XPlatformAssay3 extends BioAssay {
  
  public XPlatformAssay3() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("B");
    
    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);
    
    int split1 = builder.createSplitOperation();
    builder.connect(merge1, split1);;
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
