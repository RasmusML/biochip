package dmb.run;

import dmb.algorithms.BioArray;
import dmb.algorithms.BioAssay;
import dmb.algorithms.ElectrodeActivations;
import dmb.algorithms.ElectrodeActuation;
import dmb.algorithms.ElectrodeState;
import dmb.algorithms.GreedyRouter;
import dmb.algorithms.PlatformInterface;
import dmb.algorithms.Point;
import dmb.algorithms.Router;
import dmb.algorithms.RoutingResult;
import dmb.algorithms.components.DefaultMixingPercentages;
import dmb.algorithms.components.ElectrodeActivationTranslator;
import dmb.algorithms.components.MixingPercentages;
import dmb.testbench.tests.PCRMixingTreeArray;
import dmb.testbench.tests.PCRMixingTreeAssay;

public class RunMeRealPlatform {
  
  public static void main(String[] args) {
    BioAssay assay = new PCRMixingTreeAssay();
    BioArray array = new PCRMixingTreeArray();
    
    MixingPercentages percentages = new DefaultMixingPercentages();
    
    Router router = new GreedyRouter();
    RoutingResult result = router.compute(assay, array, percentages);
    
    ElectrodeActivationTranslator translator = new ElectrodeActivationTranslator(array.width, array.height);
    ElectrodeActivations[] sections = translator.translateStateful(result.droplets, result.executionTime);
    
    PlatformInterface pi = new PlatformInterface();
    
    pi.connect();
    
    //pi.setHighVoltageValue(100);  // ?
    pi.turnHighVoltageOnForElectrodes();
    
    for (int i = 0; i < sections.length; i++) {
      
      System.out.printf("%d/%d\n", i, sections.length);
      
      ElectrodeActivations section = sections[i];
      for (ElectrodeActuation actuation : section.activations) {

        Point tile = actuation.tile;
        
        if (actuation.state == ElectrodeState.On) {
          pi.setElectrode(tile.x, tile.y);
        } else {
          pi.clearElectrode(tile.x, tile.y);
        }
      }
      
      sleep(100); // to give the droplets some time to actually move.
    }
    
    pi.clearAllElectrodes();
    pi.turnHighVoltageOffForElectrodes();
    
    System.out.println("done");
    
    pi.disconnect();
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}