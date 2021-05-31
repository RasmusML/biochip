package pack.run;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioAssay;
import pack.helpers.GraphvizUtil;
import pack.helpers.IOUtils;

public class RunMeGraphCreator {

	public static void main(String[] args) throws IOException, InterruptedException {
	  boolean recreateAll = true;
	  
	  List<BioAssay> assays = new ArrayList<>();
	  
	  String dir = "./src";
	  String pack = "pack/tests";
	  
	  String folderPath = String.format("%s/%s", dir, pack);
	  
	  List<String> filenames = IOUtils.getFileNames(folderPath);
	  
	  // "pack.tests.TestSuite"
	  for (String filename : filenames) {
	    String name = filename.replace(".java", "");
	    if (!name.contains("Assay")) continue;
	    
	    String fullPath = String.format("%s/%s", pack, name).replace("/", ".");
	    
	    try {
	      Class<?> clazz = Class.forName(fullPath);
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
	  
	  String graphvizPath = "C:\\Program Files (x86)\\Graphviz";

	  for (BioAssay assay : assays) {
  		String assayName = assay.getClass().getSimpleName();
  		String pngName = assayName.replaceAll("(BioAssay)|(Assay)", "");

  		String pngPath = String.format("./assays/%s.png", pngName);
  		if (!recreateAll && new File(pngPath).exists()) continue;
  		
  		String graph = assay.asGraphvizGraph();
  		GraphvizUtil.createPngFromString(graph, pngPath, graphvizPath);
  		
  		System.out.printf("\"%s\" created!\n", pngPath);
	  }
	  
	  System.out.printf("Done creating graphs.\n");
	}
}
