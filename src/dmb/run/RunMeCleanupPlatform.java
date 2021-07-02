package dmb.run;

import dmb.platform.PlatformInterface;

public class RunMeCleanupPlatform {
  
  public static void main(String[] args) {
    PlatformInterface pi = new PlatformInterface();

    pi.connect();

    pi.turnHighVoltageOnForElectrodes();
    pi.setHighVoltageValue(255);
    pi.clearAllElectrodes();

    pi.disconnect();
  }
}
