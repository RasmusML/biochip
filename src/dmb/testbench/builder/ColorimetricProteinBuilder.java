package dmb.testbench.builder;

import dmb.algorithms.Operation;
import dmb.helpers.Assert;
import framework.math.MathUtils;

/**
 * Creates colorimetric protein assays.
 */

public class ColorimetricProteinBuilder {

  private BioAssayBuilder builder;

  /**
   * Creates colorimetric protein assays.
   * 
   * @param numSamples -  number of samples
   * @param dilutionFraction - selects the dilution fraction as 1 / dilutionFraction
   */
  
  public void createColorimetricProtein(int numSamples, int dilutionFraction) {
    Assert.that(numSamples % 2 == 0);

    int expansionLevels = MathUtils.log2(numSamples);
    expansionLevels -= 1;

    int dilutionLevel = MathUtils.log2(dilutionFraction);
    int dilutionLevelLeft = dilutionLevel - expansionLevels;

    Assert.that(dilutionFraction >= 0);

    builder = new BioAssayBuilder();

    int substance = builder.createDispenseOperation("substance");
    int root = createDilutionExpansionBlock(substance);

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
      int expansion = createDilutionExpansionBlock(id);
      dilute(expansionLevelLeft - 1, dilutionLevelLeft, expansion);
      dilute(expansionLevelLeft - 1, dilutionLevelLeft, expansion);
    }
  }

  private int createDilutionExpansionBlock(int substance) {
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
    int diluter = createDilutionExpansionBlock(substance);
    int disposer = builder.createDisposeOperation();
    builder.connect(diluter, disposer);

    return diluter;
  }
}
