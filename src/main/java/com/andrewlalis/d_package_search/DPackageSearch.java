package com.andrewlalis.d_package_search;

import com.andrewlalis.d_package_search.impl.DubRegistryPackageFetcher;
import com.andrewlalis.d_package_search.impl.LucenePackageIndexer;
import com.andrewlalis.d_package_search.impl.LucenePackageSearcher;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;

public class DPackageSearch {
	public static void main(String[] args) {
		Path indexPath = Path.of("package-index");
		startIndexerThread(new IndexGenerator(
				new DubRegistryPackageFetcher(),
				() -> new LucenePackageIndexer(indexPath)
		));
		new WebApiRunner(new LucenePackageSearcher(indexPath)).run();
	}

	/**
	 * Starts a new (virtual) thread that periodically re-generates the package
	 * index.
	 * @param indexGenerator The index generator to use.
	 */
	private static void startIndexerThread(IndexGenerator indexGenerator) {
		Thread.ofVirtual().start(() -> {
			while (true) {
				System.out.println("Will re-index packages in 10 seconds");
				try {
					Thread.sleep(Duration.ofSeconds(10));
				} catch (InterruptedException e) {
					System.err.println("Indexing thread interrupted: " + e.getMessage());
					break;
				}
				System.out.println("Re-indexing packages now.");
				indexGenerator.run();
				try {
					Thread.sleep(Duration.ofMinutes(60));
				} catch (InterruptedException e) {
					System.err.println("Indexing thread interrupted: " + e.getMessage());
					break;
				}
			}
		});
	}
}
