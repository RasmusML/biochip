package pack.helpers;

import java.io.File;

public class GraphvizUtil {
  
  public static int createPngFromGvz(String gvzPath, String pngPath, String graphvizPath) {
    String command = String.format("\"%s\\bin\\dot.exe\" %s -Tpng -o %s", graphvizPath, gvzPath, pngPath);
    return ShellUtil.execute(command);
  }
  
  public static void createPngFromString(String graph, String pngPath, String graphvizPath) {
    String gvzPath = "__temporary.gvz";
    IOUtils.writeStringToFile(graph, gvzPath);
    
    int success = createPngFromGvz(gvzPath, pngPath, graphvizPath);
    
    // remove .gvz after creating the .png
    new File(gvzPath).delete();
    
    if (success != 0) {
      String error = String.format("failed to create graphviz Png.");
      throw new IllegalStateException(error);
    }
  }
}
