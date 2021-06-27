package dmb.testbench;

import dmb.algorithms.Operation;
import dmb.components.input.BioAssayBuilder;
import dmb.helpers.Assert;
import framework.math.MathUtils;

public class ColorimetricProteinBuilder {
  
  private BioAssayBuilder builder;

  public void createColorimetricProtein(int numSamples, int dilutionFraction) {
    Assert.that(numSamples % 2 == 0);
    
    int expansionLevels = MathUtils.log2(numSamples);
    expansionLevels -= 1;

    int dilutionLevel = MathUtils.log2(dilutionFraction);
    int dilutionLevelLeft = dilutionLevel - expansionLevels;
    
    Assert.that(dilutionFraction >= 0);
    
    builder = new BioAssayBuilder();

    int substance = builder.createDispenseOperation("substance");
    int root = createDilutionExpandBlock(substance);
    
    dilute(expansionLevels, dilutionLevelLeft, root);
    dilute(expansionLevels, dilutionLevelLeft, root);
  }
  
  public int getOperationCount() {
    return builder.getOperationCount();
  }
  
  public Operation[] getSink() { 
    return builder.getSink();
  }
  
  private void dilute(int expansionLevelLeft, int dilutionLevelLeft, int id) {
    if (expansionLevelLeft == 0) {
      if (dilutionLevelLeft == 0) {
        // done
      } else {
        int expansion = createDilutionBlock(id);
        dilute(0, dilutionLevelLeft - 1, expansion);
      }
      
    } else {
      int expansion = createDilutionExpandBlock(id);
      dilute(expansionLevelLeft - 1, dilutionLevelLeft, expansion);
      dilute(expansionLevelLeft - 1, dilutionLevelLeft, expansion);
    }
  }

  private int createDilutionExpandBlock(int substance) {
    int diluter = builder.createDispenseOperation("diluter");
    
    int merge = builder.createMergeOperation();
    builder.connect(substance, merge);
    builder.connect(diluter, merge);
    
    int mix = builder.createMixOperation();
    builder.connect(merge, mix);
    
    int split1 = builder.createSplitOperation();
    builder.connect(mix, split1);
    
    return split1;
  }
  
  private int createDilutionBlock(int substance) {
    int diluter = createDilutionExpandBlock(substance);
    int disposer = builder.createDisposeOperation();
    builder.connect(diluter, disposer);
    
    return diluter;
  }
}
