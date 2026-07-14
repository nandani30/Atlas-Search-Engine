package com.atlas.searchengine.service;

import org.springframework.stereotype.Service;

@Service
public class BM25Ranker {
    private static final double k1 = 1.5;
    private static final double b = 0.75;

    /**
     * Calculates the BM25 score for a term in a document.
     *
     * @param termFreq          f(qi, D): The frequency of the term in the document.
     *                          Term frequency saturation: BM25 uses a non-linear term frequency function
     *                          to prevent a single term from dominating the score just because it appears many times.
     *                          The parameter k1 controls this saturation.
     * @param docLength         |D|: The length of the document in words.
     * @param avgDocLength      avgdl: The average length of all documents in the collection.
     *                          Length normalization: Documents are penalized if they are longer than average
     *                          (since they are more likely to contain any given term by chance) and rewarded
     *                          if they are shorter. Parameter b controls the scaling of this normalization.
     * @param docCountWithTerm  n(qi): The number of documents that contain the term.
     * @param totalDocs         N: The total number of documents in the collection.
     *                          Inverse Document Frequency (IDF): Terms that appear in many documents are
     *                          less informative than terms that appear in few documents. IDF weights terms
     *                          based on their rarity.
     * @return The BM25 score for this term in the document.
     */
    public double scoreTerm(int termFreq, int docLength, double avgDocLength, int docCountWithTerm, int totalDocs) {
        if (termFreq == 0) return 0.0;

        // IDF calculation
        double idf = Math.log(((totalDocs - docCountWithTerm + 0.5) / (docCountWithTerm + 0.5)) + 1.0);

        // Term frequency with length normalization
        double tf = (termFreq * (k1 + 1)) / (termFreq + k1 * (1 - b + b * (docLength / avgDocLength)));

        return idf * tf;
    }
}
