package pack.tests;

public class SmallModuleCatalog extends ModuleCatalog {
  
  public SmallModuleCatalog() {
    register("heater90", 20, 3, 1, 2, 3);
    register("heater9000", 40, 5, 3, 2, 2);
  }
}
