package dmb.testbench.bundles;

import java.util.ArrayList;
import java.util.List;

import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.testbench.Test;
import dmb.testbench.tests.functionality.BlockingDispenserTestBioArray;
import dmb.testbench.tests.functionality.BlockingDispenserTestBioAssay;
import dmb.testbench.tests.functionality.DetectorArray1;
import dmb.testbench.tests.functionality.DetectorAssay1;
import dmb.testbench.tests.functionality.DispenseArray1;
import dmb.testbench.tests.functionality.DispenseAssay1;
import dmb.testbench.tests.functionality.DisposeArray1;
import dmb.testbench.tests.functionality.DisposeAssay1;
import dmb.testbench.tests.functionality.MergeArray1;
import dmb.testbench.tests.functionality.MergeArray2;
import dmb.testbench.tests.functionality.MergeArray3;
import dmb.testbench.tests.functionality.MergeAssay1;
import dmb.testbench.tests.functionality.MergeAssay2;
import dmb.testbench.tests.functionality.MergeAssay3;
import dmb.testbench.tests.functionality.MixArray1;
import dmb.testbench.tests.functionality.MixArray2;
import dmb.testbench.tests.functionality.MixAssay1;
import dmb.testbench.tests.functionality.MixAssay2;
import dmb.testbench.tests.functionality.ModuleBioArray1;
import dmb.testbench.tests.functionality.ModuleBioAssay1;
import dmb.testbench.tests.functionality.PlatformArray1;
import dmb.testbench.tests.functionality.PlatformAssay1;
import dmb.testbench.tests.functionality.Test1BioArray;
import dmb.testbench.tests.functionality.Test1BioAssay;

/**
 * A set of small tests focusing on testing a single operation.
 */

public class FunctionalTestBundle implements TestBundle {

  private List<Test> tests;

  public FunctionalTestBundle() {
    tests = new ArrayList<>();
    registerAllTests();
  }

  @Override
  public List<Test> get() {
    return new ArrayList<>(tests);
  }

  private void registerAllTests() {
    register(new DetectorAssay1(), new DetectorArray1());
    register(new DispenseAssay1(), new DispenseArray1());
    register(new DisposeAssay1(), new DisposeArray1());
    register(new MergeAssay1(), new MergeArray1());
    register(new MergeAssay2(), new MergeArray2());
    register(new MergeAssay3(), new MergeArray3());
    register(new MixAssay1(), new MixArray1());
    register(new MixAssay2(), new MixArray2());
    register(new BlockingDispenserTestBioAssay(), new BlockingDispenserTestBioArray());
    register(new ModuleBioAssay1(), new ModuleBioArray1());
    register(new Test1BioAssay(), new Test1BioArray());
    register(new PlatformAssay1(), new PlatformArray1());
  }

  private void register(BioAssay assay, BioArray array) {
    Test test = new Test();
    test.array = array;
    test.assay = assay;
    tests.add(test);
  }
}
