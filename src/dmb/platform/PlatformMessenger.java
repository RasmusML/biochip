package dmb.platform;

public class PlatformMessenger {

  public String turnHighVoltageOnForElectrodesMessage() {
    return String.format("hvpoe 1 1 \r");
  }

  public String turnHighVoltageOffForElectrodesMessage() {
    return String.format("hvpoe 1 0 \r");
  }

  public String setHighVoltageValueMessage(int value) {
    return String.format("shv 1 %d \r", value);
  }

  public String clearAllElectrodesMessage(int driverId) {
    return String.format("clra %d \r", driverId);
  }

  public String setElectrodeMessage(int driverId, int electrodeId) {
    return String.format("setel %d %d \r", driverId, electrodeId);
  }

  public String clearElectrodeMessage(int driverId, int electrodeId) {
    return String.format("clrel %d %d \r", driverId, electrodeId);
  }
}