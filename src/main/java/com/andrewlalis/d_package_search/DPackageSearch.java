package com.andrewlalis.d_package_search;

import com.andrewlalis.d_package_search.impl.DubRegistryPackageFetcher;
import com.andrewlalis.d_package_search.impl.LucenePackageIndexer;
import com.andrewlalis.d_package_search.impl.LucenePackageSearcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;

public class DPackageSearch {
	public static Properties APPLICATION_PROPERTIES;

	public static void main(String[] args) {
		APPLICATION_PROPERTIES = loadApplicationProperties();
		Path indexPath = Path.of("package-index");
		if (isPropTrue("indexer.enabled")) {
			startIndexerThread(new IndexGenerator(
					new DubRegistryPackageFetcher(),
					() -> new LucenePackageIndexer(indexPath)
			));
		}
		if (isPropTrue("server.enabled")) {
			new WebApiRunner(new LucenePackageSearcher(indexPath)).run();
		}
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
					Thread.sleep(Duration.ofMinutes(getIntProp("indexer.delay-minutes", 60)));
				} catch (InterruptedException e) {
					System.err.println("Indexing thread interrupted: " + e.getMessage());
					break;
				}
			}
		});
	}

	private static Properties loadApplicationProperties() {
		Properties props = new Properties();
		props.setProperty("server.port", "8080");
		props.setProperty("server.host", "localhost");
		props.setProperty("server.enabled", "true");
		props.setProperty("indexer.enabled", "true");
		props.setProperty("indexer.delay-minutes", "60");
		Path propsFilePath = Path.of("application.properties");
		if (Files.exists(propsFilePath)) {
			try (var in = Files.newInputStream(propsFilePath)) {
				props.load(in);
				System.out.println("Loaded application properties from " + propsFilePath);
			} catch (IOException e) {
				System.err.println("Failed to load application properties from " + propsFilePath + ": " + e.getMessage());
			}
		}
		return props;
	}

	public static boolean isPropTrue(String name) {
		return APPLICATION_PROPERTIES.getProperty(name, "false").equalsIgnoreCase("true");
	}

	public static int getIntProp(String name, int defaultValue) {
		String s = APPLICATION_PROPERTIES.getProperty(name, Integer.toString(defaultValue));
		return Integer.parseInt(s);
	}

	public static String getStringProp(String name) {
		return APPLICATION_PROPERTIES.getProperty(name);
	}
}
