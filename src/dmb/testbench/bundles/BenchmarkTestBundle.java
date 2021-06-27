package dmb.testbench.bundles;

import java.util.ArrayList;
import java.util.List;

import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.testbench.Test;
import dmb.testbench.tests.BlockingDispenserTestBioArray;
import dmb.testbench.tests.BlockingDispenserTestBioAssay;
import dmb.testbench.tests.ColorimetricProteinArray1;
import dmb.testbench.tests.ColorimetricProteinAssay1;
import dmb.testbench.tests.CrowdedModuleBioArray;
import dmb.testbench.tests.CrowdedModuleBioAssay;
import dmb.testbench.tests.InVitroArray1;
import dmb.testbench.tests.InVitroArray2;
import dmb.testbench.tests.InVitroAssay1;
import dmb.testbench.tests.InVitroAssay2;
import dmb.testbench.tests.ModuleBioArray1;
import dmb.testbench.tests.ModuleBioArray2;
import dmb.testbench.tests.ModuleBioArray3;
import dmb.testbench.tests.ModuleBioArray4;
import dmb.testbench.tests.ModuleBioAssay1;
import dmb.testbench.tests.ModuleBioAssay2;
import dmb.testbench.tests.ModuleBioAssay3;
import dmb.testbench.tests.ModuleBioAssay4;
import dmb.testbench.tests.PCRMixingTreeArray;
import dmb.testbench.tests.PCRMixingTreeArray2;
import dmb.testbench.tests.PCRMixingTreeAssay;
import dmb.testbench.tests.PCRMixingTreeAssay2;
import dmb.testbench.tests.PlatformArray1;
import dmb.testbench.tests.PlatformArray2;
import dmb.testbench.tests.PlatformArray3;
import dmb.testbench.tests.PlatformArray4;
import dmb.testbench.tests.PlatformAssay1;
import dmb.testbench.tests.PlatformAssay2;
import dmb.testbench.tests.PlatformAssay3;
import dmb.testbench.tests.PlatformAssay4;
import dmb.testbench.tests.Test1BioArray;
import dmb.testbench.tests.Test1BioAssay;
import dmb.testbench.tests.Test2BioArray;
import dmb.testbench.tests.Test2BioAssay;
import dmb.testbench.tests.Test3BioArray;
import dmb.testbench.tests.Test3BioAssay;
import dmb.testbench.tests.Test4BioArray;
import dmb.testbench.tests.Test4BioAssay;
import dmb.testbench.tests.Test5BioArray;
import dmb.testbench.tests.Test5BioAssay;
import dmb.testbench.tests.Test6BioArray;
import dmb.testbench.tests.Test6BioAssay;

public class BenchmarkTestBundle implements TestBundle {
  
  private List<Test> tests;
  
  public BenchmarkTestBundle() {
    tests = new ArrayList<>();
    registerAllTests();
  }
  
  @Override
  public List<Test> get() {
    return new ArrayList<>(tests);
  }
  
  private void registerAllTests() {
    register(new ColorimetricProteinAssay1(), new ColorimetricProteinArray1());
    
    register(new PCRMixingTreeAssay2(), new PCRMixingTreeArray2());

    register(new InVitroAssay1(), new InVitroArray1());
    register(new InVitroAssay2(), new InVitroArray2());

    register(new Test1BioAssay(), new Test1BioArray());
    register(new Test2BioAssay(), new Test2BioArray());
    register(new Test3BioAssay(), new Test3BioArray());
    register(new Test4BioAssay(), new Test4BioArray());
    register(new Test5BioAssay(), new Test5BioArray());
    register(new Test6BioAssay(), new Test6BioArray());
    register(new PCRMixingTreeAssay(), new PCRMixingTreeArray());
    register(new BlockingDispenserTestBioAssay(), new BlockingDispenserTestBioArray());
    register(new ModuleBioAssay1(), new ModuleBioArray1());
    register(new ModuleBioAssay2(), new ModuleBioArray2());
    register(new ModuleBioAssay3(), new ModuleBioArray3());
    register(new ModuleBioAssay4(), new ModuleBioArray4());
    register(new CrowdedModuleBioAssay(), new CrowdedModuleBioArray());
    register(new PlatformAssay1(), new PlatformArray1());
    register(new PlatformAssay2(), new PlatformArray2());
    register(new PlatformAssay3(), new PlatformArray3());
    register(new PlatformAssay4(), new PlatformArray4());
  }
  
  private void register(BioAssay assay, BioArray array) {
    Test test = new Test();
    test.array = array;
    test.assay = assay;
    tests.add(test);
  }
}
