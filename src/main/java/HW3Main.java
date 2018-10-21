import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;
import Search.ExtractQuery;
import Search.QueryRetrievalModel;

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
		if (args.length == 1) model.setMu(Double.parseDouble(args[0]));
		System.err.println("MU used is: " + model.getMu());

		// Extract the queries.
		ExtractQuery queries = new ExtractQuery();

		long startTime = System.currentTimeMillis();
		while (queries.hasNext()) {
			Query aQuery = queries.next();
			System.out.println(aQuery.GetTopicId() + "\t" + aQuery.GetQueryContent());
			// Retrieve 20 candidate documents on the indexing.
			List<Document> results = model.retrieveQuery(aQuery, 20);
			if (results != null) {
				int rank = 1;
				for (Document result : results) {
					System.out.println(aQuery.GetTopicId() + " Q0 " + result.docno() + " " + rank + " " + result.score() + " MYRUN");
					rank++;
				}
			}
		}
		long endTime = System.currentTimeMillis(); // end time of running code
		System.out.println("\n\n4 queries search time: " + (endTime - startTime) / 60000.0 + " min");
		ixreader.close();
	}

}
