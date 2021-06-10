package dmb.algorithms.components;

import java.util.List;

import dmb.algorithms.Droplet;
import dmb.algorithms.DropletUnit;
import dmb.algorithms.ElectrodeActivations;
import dmb.algorithms.ElectrodeActuation;
import dmb.algorithms.ElectrodeState;
import dmb.algorithms.Point;

public class ElectrodeActivationTranslator {
  
  // droplet units may cause an electrode first to de-actuate (unit move away), then actuate (unit move onto the electrode) during the same timestep. So instead of storing both actuation and de-actuation state (which would be wrong), only store the actuation state.
  
  private int width, height;
  
  private ElectrodeState[][] previousPlatformState;
  private ElectrodeState[][] currentPlatformState;
  
  public ElectrodeActivationTranslator(int width, int height) {
    this.width = width;
    this.height = height;
    
    previousPlatformState = new ElectrodeState[width][height];
    currentPlatformState = new ElectrodeState[width][height];
  }
  
  public ElectrodeActivations[] translateStateless(List<Droplet> droplets, int timesteps) {
    ElectrodeActivations[] sections = new ElectrodeActivations[timesteps];
    
    clear(currentPlatformState);
  
    for (int time = 0; time < timesteps; time++) {
      for (Droplet droplet : droplets) {
        for (DropletUnit unit : droplet.units) {
          Point tile = unit.route.getPosition(time);
          if (tile == null) continue;
          
          currentPlatformState[tile.x][tile.y] = ElectrodeState.On;
        }
      }
      
      ElectrodeActivations section = buildStatelessSection();
      sections[time] = section;
      
      clear(currentPlatformState);
    }

    return sections;
  }
  
  public ElectrodeActivations[] translateStateful(List<Droplet> droplets, int timesteps) {
    ElectrodeActivations[] sections = new ElectrodeActivations[timesteps];
    
    clear(previousPlatformState);
    clear(currentPlatformState);
  
    for (int time = 0; time < timesteps; time++) {
      for (Droplet droplet : droplets) {
        for (DropletUnit unit : droplet.units) {
          Point tile = unit.route.getPosition(time);
          if (tile == null) continue;
          
          currentPlatformState[tile.x][tile.y] = ElectrodeState.On;
        }
      }
      
      ElectrodeActivations section = buildStatefulSection();
      sections[time] = section;
      
      transfer(currentPlatformState, previousPlatformState);
      clear(currentPlatformState);
    }

    return sections;
  }
  
  private void clear(ElectrodeState[][] platformState) {
    fill(platformState, ElectrodeState.Off);
  }
  
  private void fill(ElectrodeState[][] platformState, ElectrodeState state) {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        platformState[x][y] = state;
      }
    }
  }
  
  private void transfer(ElectrodeState[][] source, ElectrodeState[][] target) {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        target[x][y] = source[x][y];
      }
    }
  }
  
  private ElectrodeActivations buildStatelessSection() {
    ElectrodeActivations section = new ElectrodeActivations();
    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        
        if (currentPlatformState[x][y] == ElectrodeState.On) {
          ElectrodeActuation activation = new ElectrodeActuation();
          activation.tile = new Point(x, y);
          activation.state = ElectrodeState.On;
          section.activations.add(activation);
        }
      }
    }
    
    return section;
  }
  
  private ElectrodeActivations buildStatefulSection() {
    ElectrodeActivations section = new ElectrodeActivations();
    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        
        // only store changes.
        if (currentPlatformState[x][y] != previousPlatformState[x][y]) {
          ElectrodeActuation activation = new ElectrodeActuation();
          activation.tile = new Point(x, y);
          activation.state = currentPlatformState[x][y];
          section.activations.add(activation);
        }
      }
    }
    
    return section;
  }
}

