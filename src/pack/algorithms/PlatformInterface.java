package pack.algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import pack.helpers.Assert;
import pack.helpers.IOUtils;

public class PlatformInterface {
  
  private Map<Integer, ElectrodeMapping> idToMapping;
  private SerialTransmitter transmitter;
  private PlatformMessenger messenger;
  private Platform platform;
  
  public PlatformInterface() {
    platform = new Platform();
    platform.rows = 20;
    platform.columns = 32;
    
    transmitter = new SerialTransmitter();
    messenger = new PlatformMessenger();
    
    List<ElectrodePlatformDefinition> electrodes = loadDefinitions();
    setupElectrodeMapping(electrodes);
  }

  private void setupElectrodeMapping(List<ElectrodePlatformDefinition> electrodes) {
    idToMapping = new HashMap<>();
    
    for (ElectrodePlatformDefinition definition : electrodes) {
      int id = definition.id;

      ElectrodeMapping mapping = new ElectrodeMapping();
      mapping.electrodeId = definition.electrodeId;
      mapping.driverId = definition.driverId;
      
      idToMapping.put(id, mapping);
    }
  }
  
  public void turnHighVoltageOnForElectrodes() {
    String message = messenger.turnHighVoltageOnForElectrodesMessage();
    transmitter.send(message);
  }
  
  public void turnHighVoltageOffForElectrodes() {
    String message = messenger.turnHighVoltageOffForElectrodesMessage();
    transmitter.send(message);
  }
  
  public void clearAllElectrodes() {
    String message1 = messenger.clearAllElectrodesMessage(0);
    String message2 = messenger.clearAllElectrodesMessage(1);
    
    transmitter.send(message1);
    transmitter.send(message2);
  }
  
  public void setElectrode(int x, int y) {
    ElectrodeMapping mapping = getElectrodeMappingByXY(x, y);
    String message = messenger.setElectrodeMessage(mapping.driverId, mapping.electrodeId);
    transmitter.send(message);
  }
  
  public void clearElectrode(int x, int y) {
    ElectrodeMapping mapping = getElectrodeMappingByXY(x, y);
    String message = messenger.clearElectrodeMessage(mapping.driverId, mapping.electrodeId);
    transmitter.send(message);
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
  
  private ElectrodeMapping getElectrodeMappingByXY(int x, int y) {
    int id = getElectrodeIdByRowAndColumn(platform.rows - 1 - y, x);
    return idToMapping.get(id);
  }

  private List<ElectrodePlatformDefinition> loadDefinitions() {
    String platformDefinitionPath = "/platformMapping.json";
    String definitions = new String(IOUtils.readFileAsBytes(platformDefinitionPath));
    
    Type listType = new TypeToken<ArrayList<ElectrodePlatformDefinition>>(){}.getType();
    List<ElectrodePlatformDefinition> electrodes = new Gson().fromJson(definitions, listType);

    return electrodes;
  }
  
  public static void main(String[] args) {
    new PlatformInterface();
  }
}

class SerialTransmitter {
  
  public void send(String message) {
    System.out.println(message);
  }
}

class ElectrodePlatformDefinition {
  @SerializedName(value = "id", alternate = {"ID"})
  public int id;
  
  @SerializedName(value = "electrodeId", alternate = {"electrodeID"})
  public int electrodeId;
  
  @SerializedName(value = "driverId", alternate = {"driverID"})
  public int driverId;
}

class ElectrodeMapping {
  public int electrodeId;
  public int driverId;
}

class Platform {
  public int rows;
  public int columns;
}

class PlatformMessenger {
  
  public String turnHighVoltageOnForElectrodesMessage() {
    return String.format("hvpoe 1 1 \r");
  }
  
  public String turnHighVoltageOffForElectrodesMessage() {
    return String.format("hvpoe 1 0 \r");
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