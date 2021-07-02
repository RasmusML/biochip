package dmb.run;

import dmb.actuation.ElectrodeActivationTranslator;
import dmb.actuation.ElectrodeActivations;
import dmb.actuation.ElectrodeActuation;
import dmb.actuation.ElectrodeState;
import dmb.algorithms.DropletSizeAwareGreedyRouter;
import dmb.algorithms.Point;
import dmb.algorithms.Router;
import dmb.algorithms.RoutingResult;
import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.mixingpercentages.DefaultMixingPercentages;
import dmb.components.mixingpercentages.MixingPercentages;
import dmb.platform.PlatformInterface;
import dmb.testbench.tests.platform.XPlatformArray4;
import dmb.testbench.tests.platform.XPlatformAssay4;

public class RunMeRealPlatform {

  public static void main(String[] args) {
    BioAssay assay = new XPlatformAssay4();
    BioArray array = new XPlatformArray4();

    MixingPercentages percentages = new DefaultMixingPercentages();

    Router router = new DropletSizeAwareGreedyRouter();
    RoutingResult result = router.compute(assay, array, percentages);

    ElectrodeActivationTranslator translator = new ElectrodeActivationTranslator(array.width, array.height);
    ElectrodeActivations[] sections = translator.translateStateful(result.droplets, result.executionTime);

    PlatformInterface pi = new PlatformInterface();

    pi.connect();

    pi.turnHighVoltageOnForElectrodes();
    pi.setHighVoltageValue(230);
    pi.clearAllElectrodes();

    for (int i = 0; i < sections.length; i++) {

      System.out.printf("%d/%d\n", i, sections.length);

      ElectrodeActivations section = sections[i];
      for (ElectrodeActuation actuation : section.activations) {

        Point tile = actuation.tile;

        if (actuation.state == ElectrodeState.On) {
          // first clear
        } else {
          pi.clearElectrode(tile.x, tile.y);
        }
      }
      
      for (ElectrodeActuation actuation : section.activations) {
        Point tile = actuation.tile;

        if (actuation.state == ElectrodeState.On) {
          pi.setElectrode(tile.x, tile.y);
        }
      }

      sleep(400); // to give the droplets some time to actually move.
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
