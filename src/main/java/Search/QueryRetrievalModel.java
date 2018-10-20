package Search;

import Classes.Document;
import Classes.Query;
import IndexingLucene.MyIndexReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QueryRetrievalModel {

    private final MyIndexReader indexReader;
    private final int indexCorpusSize;
    private double mu = 2000;
    private HashMap<String, Long> collectionFreq = new HashMap<>();

    public QueryRetrievalModel(MyIndexReader ixreader) {
        indexReader = ixreader;
        this.indexCorpusSize = ixreader.getTotalNumofCorpus();
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

        return internalQueryDocument(queryTokens, TopN);
    }

    /**
     * Internal method for querying one tokenized document
     */
    private List<Document> internalQueryDocument(String[] tokens, int topN) throws IOException {
        HashMap<Integer, HashMap<String, Integer>> queryResult = populateQueryResult(tokens);
        ArrayList<Document> allResults = queryLikelihood(queryResult, tokens);

        // Order result by score
        allResults.sort((doc1, doc2) -> {
            // DESC ordering -> (d1 <= d2)
            double d1s = doc1.score(), d2s = doc2.score();
            return d1s > d2s ? -1 : 1;
        });

        // Pick top N results
        int finalSize = Math.min(topN, allResults.size());
        ArrayList<Document> res = new ArrayList<>(finalSize);
        int countDoc = 0;
        for (Document doc : allResults) {
            res.add(doc);
            if (++countDoc > finalSize - 1) break;
        }
        // Save memory
        queryResult.clear();
        allResults.clear();
        return res;
    }

    private HashMap<Integer, HashMap<String, Integer>> populateQueryResult(String[] tokens) throws IOException {
        // <DOCID, <TERM, FREQ>>
        HashMap<Integer, HashMap<String, Integer>> res = new HashMap<>();
        for (String token : tokens) {
            if (!this.collectionFreq.containsKey(token)) {
                // This step costs time, so have to cache it in a HashMap first
                Long cf = this.indexReader.CollectionFreq(token);
                // Show a warning about detecting non-exist term token
                if (cf.equals(0L))
                    System.err.println(String.format("[WARN] Token <%s> not in collection", token));
                this.collectionFreq.put(token, cf);
            }
            Long cFreq = this.collectionFreq.get(token);
            // Non-exist, no need to calc posting list
            if (cFreq.equals(0L)) continue;
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

    /**
     * Use LM method for calc the query result score
     */
    private ArrayList<Document> queryLikelihood(HashMap<Integer, HashMap<String, Integer>> queryResult, String[] tokens)
            throws IOException {
        ArrayList<Document> allResults = new ArrayList<>(queryResult.size());
        for (Integer docid : queryResult.keySet()) {
            HashMap<String, Integer> docTermFreqList = queryResult.get(docid);
            double score = 1.0;
            int doclen = indexReader.docLength(docid);
            // Dirichlet smoothing (Reference: org.apache.lucene.search.similarities.LMDirichletSimilarity)
            double adjLen = (doclen + this.mu);
            double // (|D|/(|D|+MU)) as l1 and (MU/(|D|+MU)) as r1
                    l1 = 1.0 * doclen / adjLen,
                    r1 = 1.0 * this.mu / adjLen;
            for (String token : tokens) {
                Long cf = this.collectionFreq.get(token);
                // Non-exist, no need to calc rest
                if (cf.equals(0L)) continue;
                int tf = docTermFreqList.getOrDefault(token, 0);
                double // p(w|D) = l1*(c(w,D)/|D|) + r1*p(w|REF)
                        l2 = 1.0 * tf / doclen,
                        r2 = 1.0 * cf / this.indexCorpusSize;
                score *= (l1 * l2 + r1 * r2);
            }
            score = score > 0 ? score : 0;
            allResults.add(new Document(docid.toString(), this.indexReader.getDocno(docid), score));
        }
        return allResults;
    }

}
