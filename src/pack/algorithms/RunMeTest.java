package pack.algorithms;

import java.io.IOException;
import java.util.List;

public class RunMeTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		BioAssay assay = new MergeBioAssay();
		BioArray array = new MergeBioArray();
		MixingPercentages percentages = new MixingPercentages();
		
		assay.saveAsPng();
		
		MergeRouter router = new MergeRouter();
		List<Route> routes = router.compute(assay, array, percentages);
	}
}
