package pack.algorithms;

import java.util.List;

public abstract class MoveFinder {

  // a couple of overloads with "default" arguments for convenience.
  
  public List<Move> getValidMoves(Droplet droplet, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, null, null, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, mergeSibling, null, timestamp, droplets, modules, array);
  }
  
  public List<Move> getValidMoves(Droplet droplet, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, null, module, timestamp, droplets, modules, array);
  }
 
  public abstract List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array);
  public abstract List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array);

}
