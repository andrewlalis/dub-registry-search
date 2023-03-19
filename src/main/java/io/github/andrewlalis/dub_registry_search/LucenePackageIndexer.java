package io.github.andrewlalis.dub_registry_search;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class LucenePackageIndexer implements PackageIndexer, AutoCloseable {
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
	public void addToIndex(ObjectNode packageJson) throws IOException {
		String registryId = packageJson.get("_id").asText();
		String name = packageJson.get("name").asText();
		String dubUrl = "https://code.dlang.org/packages/" + name;

		Document doc = new Document();
		doc.add(new StoredField("registryId", registryId));
		doc.add(new TextField("name", name, Field.Store.YES));
		doc.add(new StoredField("dubUrl", dubUrl));
	}

	@Override
	public void close() throws Exception {
		indexWriter.close();
		analyzer.close();
		dir.close();
	}
}
