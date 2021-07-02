package dmb.platform;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import dmb.helpers.Assert;
import dmb.helpers.IOUtils;

public class PlatformInterface {

  private Map<Integer, ElectrodeMapping> idToMapping;
  private Transmitter transmitter;
  private PlatformMessenger messenger;
  private Platform platform;

  private int commandDelay;

  public PlatformInterface() {
    platform = new Platform();

    transmitter = new SerialTransmitter();
    messenger = new PlatformMessenger();

    commandDelay = 50;

    List<ElectrodePlatformDefinition> electrodes = loadDefinitions();
    setupElectrodeMappings(electrodes);
  }

  public void connect() {
    String port = "COM3";
    transmitter.open(port);
  }

  public void disconnect() {
    transmitter.close();
  }

  public void turnHighVoltageOnForElectrodes() {
    Assert.that(!platform.highVoltageOn, "High voltage is already on!");

    String message = messenger.turnHighVoltageOnForElectrodesMessage();
    transmitter.send(message);
    commandWait();

    platform.highVoltageOn = true;
  }

  public void turnHighVoltageOffForElectrodes() {
    Assert.that(platform.highVoltageOn, "High voltage is not on, so you can't turn high voltage off!");

    String message = messenger.turnHighVoltageOffForElectrodesMessage();
    transmitter.send(message);
    commandWait();

    platform.highVoltageOn = false;
  }

  public void setHighVoltageValue(int value) {
    Assert.that(value >= 0, "value should be non-negative.");

    String message = messenger.setHighVoltageValueMessage(value);
    transmitter.send(message);
    commandWait();
  }

  public void clearAllElectrodes() {
    platform.clearElectrodes();

    String message1 = messenger.clearAllElectrodesMessage(0);
    String message2 = messenger.clearAllElectrodesMessage(1);

    transmitter.send(message1);
    commandWait();

    transmitter.send(message2);
    commandWait();
  }

  public void setElectrode(int x, int y) {
    Assert.that(!platform.isElectrodeOnByXY(x, y), String.format("electrode (x,y)=(%d,%d) is already on!", x, y));

    ElectrodeMapping mapping = getElectrodeMappingByXY(x, y);
    String message = messenger.setElectrodeMessage(mapping.driverId, mapping.electrodeId);
    transmitter.send(message);
    platform.flipElectrodeStateByXY(x, y);

    commandWait();
  }

  public void clearElectrode(int x, int y) {
    Assert.that(platform.isElectrodeOnByXY(x, y), String.format("electrode (x,y)=(%d,%d) is not on and cannot be cleared!", x, y));

    ElectrodeMapping mapping = getElectrodeMappingByXY(x, y);
    String message = messenger.clearElectrodeMessage(mapping.driverId, mapping.electrodeId);
    transmitter.send(message);
    platform.flipElectrodeStateByXY(x, y);

    commandWait();
  }

  private int getElectrodeIdByRowAndColumn(int row, int column) {
    Assert.that(row >= 0 && row <= platform.rows - 1);
    Assert.that(column >= 0 && column <= platform.columns - 1);
    return row * platform.columns + column + 1;
  }

  /*
  private ElectrodeMapping getElectrodeMappingByRowAndColumn(int row, int column) {
    int id = getElectrodeIdByRowAndColumn(row, column);
    return idToMapping.get(id);
  }
  */

  private void commandWait() {
    sleepByMinimum(commandDelay);
  }

  // just because we ask to sleep for X ms, doesn't mean we actually do. It is up to the OS to decide, how long we actually sleep.
  private void sleepByMinimum(long ms) {
    long start = System.currentTimeMillis();

    sleep(ms);

    long prev = start;
    long now = System.currentTimeMillis();

    long dt = now - prev;
    long left = ms - dt;

    while (left > 0) {
      sleep(left);

      prev = now;
      now = System.currentTimeMillis();
      dt = now - prev;

      left -= dt;
    }

  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private ElectrodeMapping getElectrodeMappingByXY(int x, int y) {
    int id = getElectrodeIdByRowAndColumn(platform.rows - 1 - y, x);
    return idToMapping.get(id);
  }

  private List<ElectrodePlatformDefinition> loadDefinitions() {
    String platformDefinitionPath = "/platformMapping.json";
    String definitions = new String(IOUtils.readFileAsBytes(platformDefinitionPath));

    Type listType = new TypeToken<ArrayList<ElectrodePlatformDefinition>>() {
    }.getType();
    List<ElectrodePlatformDefinition> electrodes = new Gson().fromJson(definitions, listType);

    return electrodes;
  }

  private void setupElectrodeMappings(List<ElectrodePlatformDefinition> definitions) {
    idToMapping = new HashMap<>();

    for (ElectrodePlatformDefinition definition : definitions) {
      int id = definition.id;

      ElectrodeMapping mapping = new ElectrodeMapping();
      mapping.electrodeId = definition.electrodeId;
      mapping.driverId = definition.driverId;

      idToMapping.put(id, mapping);
    }
  }

  public static void main(String[] args) {
    PlatformInterface pi = new PlatformInterface();

    pi.turnHighVoltageOnForElectrodes();

    pi.setElectrode(3, 2);
    pi.setElectrode(0, 19);
    pi.clearElectrode(0, 19);
    pi.setElectrode(0, 19);

    pi.clearAllElectrodes();

    pi.turnHighVoltageOffForElectrodes();
  }
}

interface Transmitter {
  public void open(String port);

  public void send(String message);

  public void close();
}

class TerminalTransmitter implements Transmitter {

  @Override
  public void open(String port) {
  }

  @Override
  public void send(String message) {
    System.out.print(message);
  }

  @Override
  public void close() {
  }

}

class SerialTransmitter implements Transmitter {

  private SerialPort serialPort;

  @Override
  public void open(String port) {
    SerialPort[] ports = SerialPort.getCommPorts();
    
    for (SerialPort portName : ports) {
      System.out.println(portName.getPortDescription());
    }
    
    serialPort = SerialPort.getCommPort(port);

    serialPort.setBaudRate(115200); 
    serialPort.setNumDataBits(8);
    serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
    serialPort.setParity(SerialPort.NO_PARITY);

    serialPort.openPort();
    
    System.out.printf("connected to \"%s\"\n", serialPort.getPortDescription());
  }

  @Override
  public void send(String message) {
    byte[] buffer = message.getBytes();
    serialPort.writeBytes(buffer, buffer.length);
    System.out.printf("sending \"%s\"\n", message);
  }

  @Override
  public void close() {
    serialPort.closePort();
  }
}

class ElectrodePlatformDefinition {
  @SerializedName(value = "id", alternate = { "ID" })
  public int id;

  @SerializedName(value = "electrodeId", alternate = { "electrodeID" })
  public int electrodeId;

  @SerializedName(value = "driverId", alternate = { "driverID" })
  public int driverId;
}

class ElectrodeMapping {
  public int electrodeId;
  public int driverId;
}
