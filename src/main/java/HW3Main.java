import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;
import Search.ExtractQuery;
import Search.QueryRetrievalModel;

import java.util.Arrays;
import java.util.List;

/**
 * !!! YOU CANNOT CHANGE ANYTHING IN THIS CLASS !!!
 *
 * Main class for running your HW3.
 *
 */
public class HW3Main {

	public static void main(String[] args) throws Exception {
		// Initialization.
		MyIndexReader ixreader = new MyIndexReader("trectext");
		QueryRetrievalModel model = new QueryRetrievalModel(ixreader);
		double[] muList = new double[]{2000.0};
		if (args.length == 1)
			muList = Arrays.stream(args[0].split(",")).mapToDouble(Double::parseDouble).toArray();
		for (double mu : muList) {
			model.setMu(mu);
			System.out.println("MU used is: " + model.getMu());
			// Extract the queries.
			ExtractQuery queries = new ExtractQuery();

			long startTime = System.currentTimeMillis();
			while (queries.hasNext()) {
				Query aQuery = queries.next();
				System.out.println(aQuery.GetTopicId() + "\t" + aQuery.GetQueryContent());
				// Retrieve 20 candidate documents on the indexing.
				List<Document> results = model.retrieveQuery(aQuery, 3);
				if (results != null) {
					int rank = 1;
					for (Document result : results) {
						System.out.println(aQuery.GetTopicId() + " Q0 " + result.docno() + " " + rank + " " + result.score() + " TESTMU");
						rank++;
					}
				}
			}
			long endTime = System.currentTimeMillis(); // end time of running code
			System.out.println();System.out.println();
		}
		ixreader.close();
	}

}
