package dmb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import dmb.algorithms.DropletSizeAwareGreedyRouter;
import dmb.algorithms.Router;
import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.mixingpercentages.DefaultMixingPercentages;
import dmb.components.mixingpercentages.MixingPercentages;
import dmb.testbench.tests.BlockingDispenserTestBioArray;
import dmb.testbench.tests.BlockingDispenserTestBioAssay;
import dmb.testbench.tests.CrowdedModuleBioArray;
import dmb.testbench.tests.CrowdedModuleBioAssay;
import dmb.testbench.tests.ModuleBioArray2;
import dmb.testbench.tests.ModuleBioAssay2;
import dmb.testbench.tests.PCRMixingTreeArray;
import dmb.testbench.tests.PCRMixingTreeAssay;
import dmb.testbench.tests.PlatformArray4;
import dmb.testbench.tests.PlatformAssay4;
import dmb.testbench.tests.Test3BioArray;
import dmb.testbench.tests.Test3BioAssay;
import dmb.testbench.tests.Test6BioArray;
import dmb.testbench.tests.Test6BioAssay;
import dmb.testbench.tests.functionality.DetectorArray1;
import dmb.testbench.tests.functionality.DetectorAssay1;
import dmb.testbench.tests.functionality.DisposeArray1;
import dmb.testbench.tests.functionality.DisposeAssay1;
import dmb.testbench.tests.functionality.MergeArray3;
import dmb.testbench.tests.functionality.MergeAssay3;
import framework.scenes.Scene;

public class SelectionScene extends Scene {
  
  private JPanel root;
  private Shared shared;
  
  public SelectionScene(Shared shared) {
    this.shared = shared;
  }
  
  @Override
  public void init() {
    root = new JPanel();
    
    BoxLayout layout = new BoxLayout(root, BoxLayout.Y_AXIS);
    root.setLayout(layout);

    Dimension buttonSize = new Dimension(225, 40);
    
    JButton run = new JButton("Run");
    JButton replay = new JButton("Replay");
    
    String[] items = new String[] {"a", "b", "c", "d", "e", "f"};
    
    JComboBox<String> testSelector = new JComboBox<>(items);

    run.setPreferredSize(buttonSize);
    run.setMinimumSize(buttonSize);
    run.setSize(buttonSize);
    run.setMaximumSize(buttonSize);
    run.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        shared.result = shared.router.compute(shared.assay, shared.array, shared.mixingPercentages);
        replay.setEnabled(true);

        Color color;
        if (shared.result.completed) {
          color = Color.green;
        } else {
          color = Color.red;
        }
        
        replay.setBackground(color);
      }
    });
    
    replay.setEnabled(false);
    replay.setPreferredSize(buttonSize);
    replay.setMinimumSize(buttonSize);
    replay.setSize(buttonSize);
    replay.setMaximumSize(buttonSize);
    replay.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        manager.changeScene("replay");
      }
    });
    
    testSelector.setPreferredSize(buttonSize);
    testSelector.setMinimumSize(buttonSize);
    testSelector.setSize(buttonSize);
    testSelector.setMaximumSize(buttonSize);
    
    run.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    replay.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    testSelector.setAlignmentX(JComponent.CENTER_ALIGNMENT);

    root.add(Box.createRigidArea(new Dimension(0, 10)));
    root.add(run);
    root.add(Box.createRigidArea(new Dimension(0, 10)));
    root.add(replay);
    root.add(Box.createRigidArea(new Dimension(0, 10)));
    root.add(testSelector);
    
    
    
    BioAssay assay;
    BioArray array;

    MixingPercentages percentages = new DefaultMixingPercentages();

    assay = new DisposeAssay1();
    array = new DisposeArray1();
    
    assay = new BlockingDispenserTestBioAssay();
    array = new BlockingDispenserTestBioArray();

    assay = new Test3BioAssay();
    array = new Test3BioArray();
    
    assay = new CrowdedModuleBioAssay();
    array = new CrowdedModuleBioArray();

    assay = new PCRMixingTreeAssay();
    array = new PCRMixingTreeArray();

    assay = new ModuleBioAssay2();
    array = new ModuleBioArray2();

    assay = new PlatformAssay4();
    array = new PlatformArray4();

    assay = new MergeAssay3();
    array = new MergeArray3();
    
    assay = new DetectorAssay1();
    array = new DetectorArray1();
    
    assay = new Test6BioAssay();
    array = new Test6BioArray();
    
    /*
    assay = new Test6BioAssay();
    array = new ModuleBioArray2();
    */
    
    Router router = new DropletSizeAwareGreedyRouter();
    //router = new GreedyRouter();
    
    shared.array = array;
    shared.assay = assay;
    shared.mixingPercentages = percentages;
    shared.router = router;
  }
  
  @Override
  public void enter() {
    app.setRoot(root);
    app.attachInputListenersToComponent(root);
    
    //shared.result = shared.router.compute(shared.assay, shared.array, shared.mixingPercentages);
    //manager.changeScene("replay");  // @fix
  }
}
