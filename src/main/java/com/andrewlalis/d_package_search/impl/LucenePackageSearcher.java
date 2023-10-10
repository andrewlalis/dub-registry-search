package com.andrewlalis.d_package_search.impl;

import com.andrewlalis.d_package_search.PackageSearchResult;
import com.andrewlalis.d_package_search.PackageSearcher;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FeatureField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * A package searcher implementation that uses a weighted wildcard query to
 * search a Lucene index.
 */
public class LucenePackageSearcher implements PackageSearcher {
    /**
     * Factor by which we prefer results containing the entire search phrase
     * instead of just a part of it.
     */
    private static final float PHRASE_WEIGHT_MODIFIER = 2f;

    /**
     * A mapping of indexed fields, and the weight they contribute to a result's
     * score, if the result contains a match for the field.
     */
    private static final Map<String, Float> WEIGHTED_FIELDS = Map.of(
            "name", 1f,
            "description", 0.5f,
            "readme", 0.25f
    );

    private final Path indexPath;

    public LucenePackageSearcher(Path indexPath) {
        this.indexPath = indexPath;
    }

    @Override
    public SequencedCollection<PackageSearchResult> search(String query) {
        if (query == null || query.isBlank() || Files.notExists(indexPath)) return Collections.emptyList();
        Query luceneQuery = buildQuery(query);

        try (DirectoryReader dirReader = DirectoryReader.open(FSDirectory.open(indexPath))) {
            IndexSearcher searcher = new IndexSearcher(dirReader, Executors.newVirtualThreadPerTaskExecutor());
            TopDocs topDocs = searcher.search(luceneQuery, 25, Sort.RELEVANCE, false);
            List<PackageSearchResult> results = new ArrayList<>(25);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(prepareResult(
                        doc,
                        "Search result scoring explanation:\n" +
                        searcher.explain(luceneQuery, scoreDoc.doc).toString()
                ));
            }
            return results;
        } catch (IOException e) {
            System.err.println("An IOException occurred while reading index: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Builds the Lucene search query for a given textual query string.
     * @param queryText The query text to use.
     * @return The query to use.
     */
    private Query buildQuery(String queryText) {
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        String[] searchTerms = queryText.toLowerCase().split("\\s+");

        // Only consider the first 5 search terms, and add a prefix query for each term for them.
        for (int i = 0; i < Math.min(5, searchTerms.length); i++) {
            for (var entry : WEIGHTED_FIELDS.entrySet()) {
                String fieldName = entry.getKey();
                float fieldWeight = entry.getValue();
                Query termQuery = new BoostQuery(new PrefixQuery(new Term(fieldName, searchTerms[i])), fieldWeight);
                queryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
        }

        /*
        If there's more than one word in the search query, put an extra emphasis
        on finding a match with the entire query together. We use the PhraseQuery
        builder to build an ordered phrase query for each of the weighted fields.
         */
        if (searchTerms.length > 1) {
            for (var entry : WEIGHTED_FIELDS.entrySet()) {
                String fieldName = entry.getKey();
                float fieldWeight = entry.getValue();
                PhraseQuery.Builder phraseQueryBuilder = new PhraseQuery.Builder();
                for (int i = 0; i < searchTerms.length; i++) {
                    phraseQueryBuilder.add(new Term(fieldName, searchTerms[i]), i);
                }
                queryBuilder.add(new BoostQuery(phraseQueryBuilder.build(), fieldWeight * PHRASE_WEIGHT_MODIFIER), BooleanClause.Occur.SHOULD);
            }
        }

        Query baseQuery = queryBuilder.build();
        System.out.println("Query: " + baseQuery.toString());
        return new BooleanQuery.Builder()
                .add(baseQuery, BooleanClause.Occur.MUST)
                .add(FeatureField.newSaturationQuery("features", "recency", 0.25f, 1f/30f), BooleanClause.Occur.SHOULD)
                .add(FeatureField.newSaturationQuery("features", "downloads", 0.5f, 500f), BooleanClause.Occur.SHOULD)
                .build();
    }

    private PackageSearchResult prepareResult(Document doc, String explanation) {
        return new PackageSearchResult(
                doc.get("name"),
                doc.get("url"),
                explanation
        );
    }
}
