package dmb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import dmb.algorithms.DropletSizeAwareGreedyRouter;
import dmb.algorithms.GreedyRouter;
import dmb.algorithms.Router;
import dmb.algorithms.RoutingResult;
import dmb.components.mixingpercentages.DefaultMixingPercentages;
import dmb.components.mixingpercentages.MixingPercentages;
import dmb.testbench.Test;
import dmb.testbench.bundles.BenchmarkTestBundle;
import dmb.testbench.bundles.TestBundle;
import framework.scenes.Scene;

public class SelectionScene extends Scene {
  
  private JPanel root;
  private Shared shared;
  
  private String selectedRouter;
  private String selectedTest;
  
  private Color defaultButtonColor;
  
  private final String greedyRouterName = "Greedy Router";
  private final String dropletSizeAwareGreedyRouterName = "Droplet Size-aware Greedy Router";
  
  public SelectionScene(Shared shared) {
    this.shared = shared;
  }
  
  @Override
  public void init() {
    TestBundle bundle = new BenchmarkTestBundle();
    List<Test> tests = bundle.get();
    List<String> bundleNames = extractTestNames(tests);
    
    MixingPercentages percentages = new DefaultMixingPercentages();
    
    createGUI(tests, bundleNames, percentages);

    /*
    BioAssay assay;
    BioArray array;

    assay = new Test6BioAssay();
    array = new Test6BioArray();
    
    Router router = new DropletSizeAwareGreedyRouter();
    //router = new GreedyRouter();
    
    shared.array = array;
    shared.assay = assay;
    shared.mixingPercentages = percentages;
    shared.router = router;
    shared.result = shared.router.compute(shared.assay, shared.array, shared.mixingPercentages);
    */
  }

  private void createGUI(List<Test> tests, List<String> bundleNames, MixingPercentages percentages) {
    root = new JPanel();
    
    BoxLayout layout = new BoxLayout(root, BoxLayout.Y_AXIS);
    
    root.setLayout(layout);

    Dimension buttonSize = new Dimension(225, 40);
    
    JButton solveButton = new JButton("Solve");
    JButton visualizeButton = new JButton("Visualize");
    
    String[] routerNames = new String[] {greedyRouterName, dropletSizeAwareGreedyRouterName};
    JComboBox<String> routerSelector = new JComboBox<>(routerNames);
    
    String[] testNames = new String[bundleNames.size()];
    bundleNames.toArray(testNames);
    
    JComboBox<String> testSelector = new JComboBox<>(testNames);
    
    selectedTest = (String) testSelector.getSelectedItem();
    selectedRouter = (String) routerSelector.getSelectedItem();

    Color color = solveButton.getBackground();
    defaultButtonColor = new Color(color.getRGB());
        
    solveButton.setPreferredSize(buttonSize);
    solveButton.setMinimumSize(buttonSize);
    solveButton.setSize(buttonSize);
    solveButton.setMaximumSize(buttonSize);
    solveButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        String routerName = (String) routerSelector.getSelectedItem();
        Router router = getRouter(routerName);

        int testIndex = testSelector.getSelectedIndex();
        Test test = tests.get(testIndex);
        
        RoutingResult result = router.compute(test.assay, test.array, percentages);

        shared.router = router;
        shared.array = test.array;
        shared.assay = test.assay;
        shared.result = result;
        
        Color color;
        if (result.completed) {
          color = Color.green;
        } else {
          color = Color.red;
        }
        
        solveButton.setEnabled(false);

        visualizeButton.setBackground(color);
        visualizeButton.setEnabled(true);
      }

    });
    
    visualizeButton.setEnabled(false);
    visualizeButton.setPreferredSize(buttonSize);
    visualizeButton.setMinimumSize(buttonSize);
    visualizeButton.setSize(buttonSize);
    visualizeButton.setMaximumSize(buttonSize);
    visualizeButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        manager.changeScene("replay");
      }
    });
    
    routerSelector.setPreferredSize(buttonSize);
    routerSelector.setMinimumSize(buttonSize);
    routerSelector.setSize(buttonSize);
    routerSelector.setMaximumSize(buttonSize);
    routerSelector.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        String selected = (String) routerSelector.getSelectedItem();
        
        if (!selected.equals(selectedRouter)) {
          selectedRouter = selected;
          
          visualizeButton.setEnabled(false);
          solveButton.setEnabled(true);
          
          visualizeButton.setBackground(defaultButtonColor);
        }
      }
    });
    
    testSelector.setPreferredSize(buttonSize);
    testSelector.setMinimumSize(buttonSize);
    testSelector.setSize(buttonSize);
    testSelector.setMaximumSize(buttonSize);
    testSelector.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        String selected = (String) testSelector.getSelectedItem();
        
        if (!selected.equals(selectedTest)) {
          selectedTest = selected;
          
          visualizeButton.setEnabled(false);
          solveButton.setEnabled(true);
          
          visualizeButton.setBackground(defaultButtonColor);
        }
      }
    });

    Dimension seperatorSize = new Dimension(225, 1);
    
    JSeparator sep = new JSeparator();
    sep.setPreferredSize(seperatorSize);
    sep.setMinimumSize(seperatorSize);
    sep.setSize(seperatorSize);
    sep.setMaximumSize(seperatorSize);    
    
    solveButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    visualizeButton.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    routerSelector.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    testSelector.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    sep.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    
    int paddingY = 10;
    
    root.add(Box.createRigidArea(new Dimension(0, paddingY)));
    root.add(routerSelector);
    root.add(Box.createRigidArea(new Dimension(0, paddingY)));
    root.add(testSelector);
    root.add(Box.createRigidArea(new Dimension(0, paddingY)));
    root.add(solveButton);

    root.add(Box.createRigidArea(new Dimension(0, paddingY)));
    root.add(sep);
    
    root.add(Box.createRigidArea(new Dimension(0, paddingY)));
    root.add(visualizeButton);
  }

  @Override
  public void enter() {
    app.setRoot(root);
    app.attachInputListenersToComponent(root);
    
    //manager.changeScene("replay");  // @fix
  }
  
  private List<String> extractTestNames(List<Test> tests) {
    List<String> names = new ArrayList<>();
    
    for (Test test : tests) {
      String assayName = test.assay.getClass().getSimpleName();
      String testName = assayName.replaceAll("(BioAssay)|(Assay)", "");
      names.add(testName);
    }
    
    return names;
  }
  
  private Router getRouter(String routerName) {
    if (routerName.equals(greedyRouterName)) return new GreedyRouter();
    else if (routerName.equals(dropletSizeAwareGreedyRouterName)) return new DropletSizeAwareGreedyRouter();
    throw new IllegalStateException("unknown router!");
  }

}
