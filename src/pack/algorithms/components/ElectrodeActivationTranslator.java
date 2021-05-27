package pack.algorithms.components;

import java.util.List;

import pack.algorithms.Droplet;
import pack.algorithms.ElectrodeActuation;
import pack.algorithms.ElectrodeActivations;
import pack.algorithms.ElectrodeState;
import pack.algorithms.Point;

public class ElectrodeActivationTranslator {
  
  public ElectrodeActivations[] translateStateless(List<Droplet> droplets, int timesteps) {
    ElectrodeActivations[] sections = new ElectrodeActivations[timesteps];
    
    for (int i = 0; i < sections.length; i++) {
      sections[i] = new ElectrodeActivations();
    }
    
    for (Droplet droplet : droplets) {
      for (int i = 0; i < droplet.route.path.size(); i++) {
        int time = droplet.route.start + i;
        Point tile = droplet.route.getPosition(time);
        
        ElectrodeActuation actuation = new ElectrodeActuation();
        actuation.tile = tile.copy();
        actuation.state = ElectrodeState.On;
        
        ElectrodeActivations section = sections[time];
        section.activations.add(actuation);
        
      }
    }

    return sections;
  }
  
  public ElectrodeActivations[] translateStateful(List<Droplet> droplets, int timesteps) {
    ElectrodeActivations[] sections = new ElectrodeActivations[timesteps];
    
    for (int i = 0; i < sections.length; i++) {
      sections[i] = new ElectrodeActivations();
    }
    
    for (Droplet droplet : droplets) {
      
      {
        int time = droplet.route.start;
        Point tile = droplet.route.getPosition(time);

        ElectrodeActuation actuation = new ElectrodeActuation();
        actuation.tile = tile.copy();
        actuation.state = ElectrodeState.On;
        
        ElectrodeActivations section = sections[time];
        section.activations.add(actuation);
      }
      
      for (int i = 1; i < droplet.route.path.size(); i++) {
        int prevTime = droplet.route.start + i - 1;
        Point prevTile = droplet.route.getPosition(prevTime);
        
        int time = droplet.route.start + i;
        Point tile = droplet.route.getPosition(time);
        
        if (tile.x != prevTile.y || tile.y != prevTile.y) {
          ElectrodeActivations section = sections[time];

          ElectrodeActuation onActivation = new ElectrodeActuation();
          onActivation.tile = tile.copy();
          onActivation.state = ElectrodeState.On;
          section.activations.add(onActivation);
          
          ElectrodeActuation offActivation = new ElectrodeActuation();
          offActivation.tile = prevTile.copy();
          offActivation.state = ElectrodeState.Off;
          section.activations.add(offActivation);
        }
      }
    }

    return sections;
  }
}

