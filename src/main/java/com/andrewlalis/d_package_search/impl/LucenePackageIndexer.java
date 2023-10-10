package com.andrewlalis.d_package_search.impl;

import com.andrewlalis.d_package_search.PackageIndexer;
import com.andrewlalis.d_package_search.PackageInfo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.*;

/**
 * An indexer that produces a Lucene index, which is a directory, composed of
 * possibly many index segments.
 */
public class LucenePackageIndexer implements PackageIndexer {
	private final IndexWriter indexWriter;
	private final Directory dir;
	private final Analyzer analyzer;

	public LucenePackageIndexer(Path indexPath) throws IOException {
		this.dir = FSDirectory.open(indexPath);
		this.analyzer = new StandardAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		config.setCommitOnClose(true);
		this.indexWriter = new IndexWriter(dir, config);
	}

	/**
	 * Adds a package to the Lucene index. This is the central place where the
	 * index's fields are defined. We define the following fields:
	 * <ul>
	 *     <li>name (text, stored)</li>
	 *     <li>url (stored only)</li>
	 *     <li>categories (multivalued string field with value for each category).</li>
	 *     <li>latestVersionTimestamp (string field with date of latest version).</li>
	 *     <li>description (optional text field)</li>
	 *     <li>license (optional string field)</li>
	 *     <li>readme (optional text field)</li>
	 *     <li>
	 *         features (feature field with the following features useful for scoring)
	 *         <ul>
	 *             <li>recency (0 - 1 value indicating how recent the package is)</li>
	 *             <li>downloads (total downloads for the package)</li>
	 *         </ul>
	 *     </li>
	 * </ul>
	 * @param info The package to index.
	 * @throws IOException If an error occurs.
	 */
	@Override
	public void addToIndex(PackageInfo info) throws IOException {
		if (info.versions().length == 0) {
			System.out.println("Skipping package \"" + info.name() + "\" because there are no versions available.");
			return;
		}
		System.out.println("Indexing package \"" + info.name() + "\".");
		String dubUrl = "https://code.dlang.org/packages/" + info.name();
		List<PackageInfo.VersionInfo> allVersions = new ArrayList<>(Arrays.asList(info.versions()));
		allVersions.sort(Comparator.comparing(PackageInfo.VersionInfo::timestamp).reversed());
		var recentVersions = allVersions.subList(0, Math.min(5, allVersions.size()));

		Document doc = new Document();
		doc.add(new TextField("name", info.name(), Field.Store.YES));
		doc.add(new StoredField("url", dubUrl));
		for (String category : info.categories()) {
			doc.add(new StringField("categories", category, Field.Store.NO));
		}

		PackageInfo.VersionInfo latestVersion = recentVersions.getFirst();
		doc.add(new StringField(
				"latestVersionTimestamp",
				DateTools.dateToString(Date.from(latestVersion.timestamp().toInstant(ZoneOffset.UTC)), DateTools.Resolution.SECOND),
				Field.Store.NO
		));
		if (latestVersion.description() != null) {
			doc.add(new TextField("description", latestVersion.description(), Field.Store.NO));
		}
		if (latestVersion.license() != null) {
			doc.add(new StringField("license", latestVersion.license(), Field.Store.NO));
		}
		if (latestVersion.readmeText() != null) {
			doc.add(new TextField("readme", latestVersion.readmeText(), Field.Store.NO));
		}

		// Add FeatureFields to score packages based on some metrics.
		int daysSinceUpdate = Math.clamp(Duration.between(latestVersion.timestamp(), info.fetchedAt()).toDays(), 1, 365 * 3);
		float recency = 1f / daysSinceUpdate;
		float downloadsScore = Math.clamp(info.totalDownloads(), 0.001f, Float.MAX_VALUE);
		doc.add(new FeatureField("features", "recency", recency));
		doc.add(new FeatureField("features", "downloads", downloadsScore));

		indexWriter.addDocument(doc);
	}

	@Override
	public void close() throws Exception {
		indexWriter.close();
		analyzer.close();
		dir.close();
	}
}
