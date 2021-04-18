package pack.algorithms;

import java.io.File;

import engine.IOUtils;
import engine.ShellUtil;

public class GraphvizUtil {
  
  public static void createPngFromGvz(String gvzPath, String pngPath, String graphvizPath) {
    String command = String.format("\"%s\\bin\\dot.exe\" %s -Tpng -o %s", graphvizPath, gvzPath, pngPath);

    int result = ShellUtil.execute(command);
    if (result != 0) {
      String error = String.format("failed to create graphviz Png.");
      throw new IllegalStateException(error);
    }
  }
  
  public static void createPngFromString(String graph, String pngPath, String graphvizPath) {
    String gvzPath = "__temporary.gvz";
    IOUtils.writeStringToFile(graph, gvzPath);
    
    createPngFromGvz(gvzPath, pngPath, graphvizPath);
    
    // remove .gvz after creating the .png
    new File(gvzPath).delete();
  }
}
