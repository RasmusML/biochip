package dmb.testbench.builder;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Operation;

public class InVitroBuilder {

  private BioAssayBuilder builder;

  public void createInVitro(int numSamples) {
    List<String> samples = createGenericSamples(numSamples);
    createInVitro(samples);
  }

  public void createInVitro(List<String> plasmaSamples) {
    builder = new BioAssayBuilder();

    String[] sugars = new String[] { "glucose", "lactate", "pyruvate" };
    //String[] sugars = new String[] { "glucose", "lactate" };

    for (String plasmaSample : plasmaSamples) {
      for (String sugar : sugars) {
        createInVitroBlock(plasmaSample, sugar);
      }
    }
  }

  public int getOperationCount() {
    return builder.getOperationCount();
  }

  public Operation[] getSink() {
    return builder.getSink();
  }

  private void createInVitroBlock(String plasmaSample, String sugarType) {
    int plasma = builder.createDispenseOperation(plasmaSample);
    int sugar = builder.createDispenseOperation(sugarType);

    int merge1 = builder.createMergeOperation();
    builder.connect(plasma, merge1);
    builder.connect(sugar, merge1);

    int mixer1 = builder.createMixOperation();
    builder.connect(merge1, mixer1);

    int detector1 = builder.createDetectionOperation("in-vitro");
    builder.connect(mixer1, detector1);

    /*
    int disposer1 = builder.createDisposeOperation();
    builder.connect(detector1, disposer1);
    */
  }

  private List<String> createGenericSamples(int numSamples) {
    List<String> samples = new ArrayList<>();

    for (int i = 0; i < numSamples; i++) {
      int id = i + 1;
      String sample = String.format("sample%d", id);
      samples.add(sample);
    }

    return samples;
  }
}
