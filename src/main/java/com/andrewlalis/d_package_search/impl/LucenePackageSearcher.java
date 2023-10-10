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
                results.add(prepareResult(doc));
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

        Map<String, Float> weightedFields = Map.of(
                "name", 1f,
                "description", 0.5f,
                "readme", 0.25f
        );

        for (int i = 0; i < Math.min(5, searchTerms.length); i++) {
            for (var entry : weightedFields.entrySet()) {
                String fieldName = entry.getKey();
                float fieldWeight = entry.getValue();
                Query termQuery = new BoostQuery(new PrefixQuery(new Term(fieldName, searchTerms[i])), fieldWeight);
                queryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
            }
        }
        Query baseQuery = queryBuilder.build();
        Query boostedQuery = new BooleanQuery.Builder()
                .add(baseQuery, BooleanClause.Occur.MUST)
                .add(FeatureField.newSaturationQuery("features", "recency"), BooleanClause.Occur.SHOULD)
                .add(FeatureField.newSaturationQuery("features", "downloads"), BooleanClause.Occur.SHOULD)
                .build();
        return boostedQuery;
    }

    private PackageSearchResult prepareResult(Document doc) {
        return new PackageSearchResult(
                doc.get("name"),
                doc.get("url")
        );
    }
}
