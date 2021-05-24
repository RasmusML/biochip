package pack.algorithms.components;

import java.util.List;

import pack.algorithms.Droplet;
import pack.algorithms.ElectrodeActivation;
import pack.algorithms.ElectrodeActivationSection;
import pack.algorithms.ElectrodeState;
import pack.algorithms.Point;

public class ElectrodeActivationTranslator {
  
  public ElectrodeActivationSection[] translateStateless(List<Droplet> droplets, int timesteps) {
    ElectrodeActivationSection[] sections = new ElectrodeActivationSection[timesteps];
    
    for (int i = 0; i < sections.length; i++) {
      sections[i] = new ElectrodeActivationSection();
    }
    
    for (Droplet droplet : droplets) {
      for (int i = 0; i < droplet.route.path.size(); i++) {
        int time = droplet.route.start + i;
        Point tile = droplet.route.getPosition(time);
        
        ElectrodeActivation activation = new ElectrodeActivation();
        activation.tile = tile.copy();
        activation.state = ElectrodeState.On;
        
        ElectrodeActivationSection section = sections[time];
        section.activations.add(activation);
        
      }
    }

    return sections;
  }
  
  public ElectrodeActivationSection[] translateStateful(List<Droplet> droplets, int timesteps) {
    ElectrodeActivationSection[] sections = new ElectrodeActivationSection[timesteps];
    
    for (int i = 0; i < sections.length; i++) {
      sections[i] = new ElectrodeActivationSection();
    }
    
    for (Droplet droplet : droplets) {
      
      {
        int time = droplet.route.start;
        Point tile = droplet.route.getPosition(time);

        ElectrodeActivation activation = new ElectrodeActivation();
        activation.tile = tile.copy();
        activation.state = ElectrodeState.On;
        
        ElectrodeActivationSection section = sections[time];
        section.activations.add(activation);
      }
      
      for (int i = 1; i < droplet.route.path.size(); i++) {
        int prevTime = droplet.route.start + i - 1;
        Point prevTile = droplet.route.getPosition(prevTime);
        
        int time = droplet.route.start + i;
        Point tile = droplet.route.getPosition(time);
        
        if (tile.x != prevTile.y || tile.y != prevTile.y) {
          ElectrodeActivationSection section = sections[time];

          ElectrodeActivation onActivation = new ElectrodeActivation();
          onActivation.tile = tile.copy();
          onActivation.state = ElectrodeState.On;
          section.activations.add(onActivation);
          
          ElectrodeActivation offActivation = new ElectrodeActivation();
          offActivation.tile = prevTile.copy();
          offActivation.state = ElectrodeState.Off;
          section.activations.add(offActivation);
        }
      }
    }

    return sections;
  }
}

