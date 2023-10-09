package com.andrewlalis.d_package_search.impl;

import com.andrewlalis.d_package_search.PackageIndexer;
import com.andrewlalis.d_package_search.PackageInfo;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

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

	@Override
	public void addToIndex(PackageInfo info) throws IOException {
		String dubUrl = "https://code.dlang.org/packages/" + info.name();

		Document doc = new Document();
		doc.add(new TextField("name", info.name(), Field.Store.YES));
		doc.add(new StoredField("url", dubUrl));
		indexWriter.addDocument(doc);
	}

	@Override
	public void close() throws Exception {
		indexWriter.close();
		analyzer.close();
		dir.close();
	}
}
