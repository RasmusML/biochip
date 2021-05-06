package pack.tests;

public class LargeModuleCatalog extends ModuleCatalog {
  
  public LargeModuleCatalog() {
    register("heater90", 20, 3, 1, 2, 3);
    register("heater9000", 40, 6, 3, 7, 7);
  }
}
