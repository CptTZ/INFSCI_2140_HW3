package Search;

import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QueryRetrievalModel {

    private MyIndexReader indexReader;
    private double mu = 2000;
    HashMap<String, Long> collectionFreq = new HashMap<>();

    public QueryRetrievalModel(MyIndexReader ixreader) {
        indexReader = ixreader;
    }

    public double getMu() {
        return mu;
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    /**
     * Search for the topic information.
     * The returned results (retrieved documents) should be ranked by the score (from the most relevant to the least).
     * TopN specifies the maximum number of results to be returned.
     * NT: you will find our IndexingLucene.Myindexreader provides method: docLength()
     * implement your retrieval model here, and for each input query, return the topN retrieved documents
     * sort the documents based on their relevance score, from high to low
     *
     * @param aQuery The query to be searched for.
     * @param TopN   The maximum number of returned document
     */
    public List<Document> retrieveQuery(Query aQuery, int TopN) throws IOException {
        String[] queryTokens = aQuery.GetQueryContent().split(" ");
        if (queryTokens.length == 0) return new ArrayList<>(0);

        return internalQueryOne(queryTokens, TopN);
    }

    /**
     * Internal method for querying one tokenized document
     */
    private List<Document> internalQueryOne(String[] tokens, int topN) throws IOException {
        HashMap<Integer, HashMap<String, Integer>> queryResult = populateQueryResult(tokens);
        List<Document> res = queryLikelihood(queryResult);

        res.sort((doc1, doc2) -> {
            // d1 <= d2, DESC
            double d1s = doc1.score(), d2s = doc2.score();
            return d1s > d2s ? -1 : 1;
        });

        return res;
    }

    private HashMap<Integer, HashMap<String, Integer>> populateQueryResult(String[] tokens) throws IOException {
        // <DOCID, <TERM, FREQ>>
        HashMap<Integer, HashMap<String, Integer>> res = new HashMap<>();
        for (String token : tokens) {
            if (!this.collectionFreq.containsKey(token)) {
                // This step costs time, so have to cache it in a HashMap first
                Long cf = this.indexReader.CollectionFreq(token);
                this.collectionFreq.put(token, cf);
            }
            Long cFreq = this.collectionFreq.get(token);
            if (cFreq.equals(0L)) {
                System.err.println(String.format("Token <%s> not in collection", token));
                continue;
            }
            int[][] postingList = this.indexReader.getPostingList(token);
            for (int[] postingForOneDoc : postingList) {
                int docid = postingForOneDoc[0], docFreq = postingForOneDoc[1];
                HashMap<String, Integer> oneTermFreq = res.getOrDefault(docid, new HashMap<>());
                if (oneTermFreq.size() == 0) {
                    res.putIfAbsent(docid, oneTermFreq);
                }
                oneTermFreq.put(token, docFreq);
            }
        }
        return res;
    }

    private List<Document> queryLikelihood(HashMap<Integer, HashMap<String, Integer>> queryResult) throws IOException {
        ArrayList<Document> res = new ArrayList<>();

        return res;
    }

}
