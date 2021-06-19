  package dmb.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dmb.components.input.BioAssay;
import dmb.helpers.GraphvizUtil;
import dmb.helpers.IOUtils;

public class RunMeGraphCreator {
  
  public static String graphvizPath = "C:\\Program Files (x86)\\Graphviz";

  public static String dir = "./src";
  public static String pack = "dmb/testbench/tests";
  
  public static boolean recreateAllGraphs = true;

	public static void main(String[] args) {
	  List<BioAssay> assays = new ArrayList<>();
	  
	  String folderPath = String.format("%s/%s", dir, pack);
	  
	  List<String> filenames = IOUtils.getFileNames(folderPath);
	  for (String filename : filenames) {
	    String name = filename.replace(".java", "");
	    String packagedPath = String.format("%s/%s", pack, name).replace("/", ".");
	    
	    try {
	      Class<?> clazz = Class.forName(packagedPath);
	      
	      // check if the class is a sub-class of BioAssay
	      if (!BioAssay.class.isAssignableFrom(clazz)) continue;
	      
	      try {
          BioAssay assay = (BioAssay) clazz.newInstance();
          assays.add(assay);
          
        } catch (IllegalAccessException | InstantiationException e) {
          e.printStackTrace();
        }
        
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
	  }
	  
	  for (BioAssay assay : assays) {
  		String assayName = assay.getClass().getSimpleName();
  		String pngName = assayName.replaceAll("(BioAssay)|(Assay)", "");

  		
  		String pngPath = String.format("./assays/%s.png", pngName);
  		if (!recreateAllGraphs && new File(pngPath).exists()) continue;
  		
  		String graph = assay.asGraphvizGraph();
  		GraphvizUtil.createPngFromString(graph, pngPath, graphvizPath);
  		
  		System.out.printf("\"%s\" created!\n", pngPath);
	  }
	  
	  System.out.printf("Done creating graphs.\n");
	}
}
