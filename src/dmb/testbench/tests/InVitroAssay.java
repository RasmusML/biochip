package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.components.input.BioAssayBuilder;

public class InVitroAssay extends BioAssay {
  
  public int createInVitro(String plasmaSample) {
    BioAssayBuilder builder = null;

    {
      int plasma1 = builder.createDispenseOperation(plasmaSample);
      int glucose = builder.createDispenseOperation("glucose");
      
      int merge1 = builder.createMergeOperation();
      builder.connect(plasma1, merge1);
      builder.connect(glucose, merge1);
      
      int mixer1 = builder.createMixOperation();
      builder.connect(merge1, mixer1);
  
      int detector1 = builder.createDetectionOperation("In-vitro");
      builder.connect(mixer1, detector1);
      
      int disposer1 = builder.createDisposeOperation();
      builder.connect(detector1, disposer1);
    }
    
    {
      int plasma2 = builder.createDispenseOperation(plasmaSample);
      int lactate = builder.createDispenseOperation("lactate");
      
      int merge1 = builder.createMergeOperation();
      builder.connect(plasma2, merge1);
      builder.connect(lactate, merge1);
      
      int mixer1 = builder.createMixOperation();
      builder.connect(merge1, mixer1);
  
      int detector1 = builder.createDetectionOperation("In-vitro");
      builder.connect(mixer1, detector1);
      
      int disposer1 = builder.createDisposeOperation();
      builder.connect(detector1, disposer1);

    }
    
    {
      int plasma3 = builder.createDispenseOperation(plasmaSample);
      int pyruvate = builder.createDispenseOperation("pyruvate");
      
      int merge1 = builder.createMergeOperation();
      builder.connect(plasma3, merge1);
      builder.connect(pyruvate, merge1);
      
      int mixer1 = builder.createMixOperation();
      builder.connect(merge1, mixer1);
  
      int detector1 = builder.createDetectionOperation("In-vitro");
      builder.connect(mixer1, detector1);
      
      int disposer1 = builder.createDisposeOperation();
      builder.connect(detector1, disposer1);
    }

    return 0;
  }
  
}
