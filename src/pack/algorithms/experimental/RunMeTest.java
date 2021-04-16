package pack.algorithms.experimental;

import java.io.IOException;
import java.util.List;

public class RunMeTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		BioAssay assay = new Test1BioAssay();
		BioArray array = new Test1BioArray();
		MixingPercentages percentages = new DefaultMixingPercentages();
		
		//assay.saveAsPng();
		
		MergeRouter router = new MergeRouter();
		List<Route> routes = router.compute(assay, array, percentages);
	}
}
