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
  
  private Map<Integer, Mapping> idToMapping;
  private SerialTransmitter transmitter;
  
  private Platform platform;
  
  public PlatformInterface() {
    platform = new Platform();
    platform.rows = 20;
    platform.columns = 32;
    
    transmitter = new SerialTransmitter();
    
    idToMapping = new HashMap<>();
    
    List<ElectrodePlatformMapping> electrodes = loadMapping();
    
    for (ElectrodePlatformMapping e : electrodes) {
      int id = e.id;

      Mapping mapping = new Mapping();
      mapping.electrodeId = e.electrodeId;
      mapping.driverId = e.driverId;
      
      idToMapping.put(id, mapping);
      
      //System.out.printf("%d %d %d\n", e.id, e.electrodeId, e.driverId);
    }
    
    
    Mapping m = getElectrodeMappingByRowAndColumn(0, 0);
    setElectrode(m.driverId, m.electrodeId);
  }
  
  public void clearAllElectrodes(int driverId) {
    String message = String.format("clra %d \r", driverId);
    transmitter.send(message);
  }
  
  public void setElectrode(int driverId, int electrodeId) {
    String message = String.format("setel %d %d \r", driverId, electrodeId);
    transmitter.send(message);
  }
  
  public void clearElectrode(int driverId, int electrodeId) {
    String message = String.format("clrel %d %d \r", driverId, electrodeId);
    transmitter.send(message);
  }
  
  public int getElectrodeId(int row, int column) {
    Assert.that(row >= 0 && row <= platform.rows - 1);
    Assert.that(column >= 0 && column <= platform.columns - 1);
    return row * platform.columns + column + 1;
  }
  
  public Mapping getElectrodeMappingByRowAndColumn(int row, int column) {
    int id = getElectrodeId(row, column);
    return idToMapping.get(id);
  }
  
  public Mapping getElectrodeMappingByXY(int x, int y) {
    int id = getElectrodeId(platform.rows - 1 - y, x);
    return idToMapping.get(id);
  }

  private List<ElectrodePlatformMapping> loadMapping() {
    String platformDefinitionPath = "/platformMapping.json";
    String definitions = new String(IOUtils.readFileAsBytes(platformDefinitionPath));
    
    Type listType = new TypeToken<ArrayList<ElectrodePlatformMapping>>(){}.getType();
    List<ElectrodePlatformMapping> electrodes = new Gson().fromJson(definitions, listType);

    return electrodes;
  }
  
  class SerialTransmitter {
    
    public void send(String message) {
      System.out.println(message);
    }
  }
  
  class ElectrodePlatformMapping {
    @SerializedName(value = "id", alternate = {"ID"})
    public int id;
    
    @SerializedName(value = "electrodeId", alternate = {"electrodeID"})
    public int electrodeId;
    
    @SerializedName(value = "driverId", alternate = {"driverID"})
    public int driverId;
  }
  
  class Mapping {
    public int electrodeId;
    public int driverId;
  }
  
  class Platform {
    public int rows;
    public int columns;
  }
  
  
  public static void main(String[] args) {
    new PlatformInterface();
  }
}

