package dmb.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

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

  }

  private void createGUI(List<Test> tests, List<String> bundleNames, MixingPercentages percentages) {
    root = new JPanel();
    
    JPanel content = new JPanel();
    
    Color contentColor = Color.white;
    
    content.setBackground(contentColor);
    content.setBorder(createContentBorder());
    content.setLayout(new GridBagLayout());

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.insets = new Insets(5, 5, 5, 5);
    constraints.fill = GridBagConstraints.BOTH;
    
    constraints.gridx = 0;
    constraints.gridy = 0;
    
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
    visualizeButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        manager.changeScene("replay");
      }
    });
    
    routerSelector.setPreferredSize(buttonSize);
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
    
    content.add(routerSelector, constraints);
    
    constraints.gridx = 0;
    constraints.gridy += 1;
    content.add(testSelector, constraints);

    constraints.gridx = 0;
    constraints.gridy += 1;
    content.add(solveButton, constraints);

    constraints.gridx = 0;
    constraints.gridy += 1;
    content.add(sep, constraints);

    constraints.gridx = 0;
    constraints.gridy += 1;
    content.add(visualizeButton, constraints);

    
    root.setLayout(new GridBagLayout());
    constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.NONE;
    constraints.anchor = GridBagConstraints.CENTER;
    root.add(content, constraints);
    
  }

  @Override
  public void enter() {
    app.setRoot(root);
    app.attachInputListenersToComponent(root);
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
  
  private Border createContentBorder() {
    Border contentBorder = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
    Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
    return BorderFactory.createCompoundBorder(contentBorder, emptyBorder);
  }
}
