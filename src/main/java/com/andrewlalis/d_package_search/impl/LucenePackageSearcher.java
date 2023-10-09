package com.andrewlalis.d_package_search.impl;

import com.andrewlalis.d_package_search.PackageSearchResult;
import com.andrewlalis.d_package_search.PackageSearcher;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;
import java.util.concurrent.Executors;

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
        for (String searchTerm : searchTerms) {
            String wildcardTerm = searchTerm + "*";
            Query basicQuery = new WildcardQuery(new Term("name", wildcardTerm));
            queryBuilder.add(new BoostQuery(basicQuery, 1f), BooleanClause.Occur.SHOULD);
        }
        return queryBuilder.build();
    }

    private PackageSearchResult prepareResult(Document doc) {
        return new PackageSearchResult(
                doc.get("name"),
                doc.get("url")
        );
    }
}
