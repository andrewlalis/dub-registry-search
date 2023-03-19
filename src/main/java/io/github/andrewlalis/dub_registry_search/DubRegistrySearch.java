package io.github.andrewlalis.dub_registry_search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class DubRegistrySearch {
	public static void main(String[] args) throws Exception {
		if (args.length == 1 && args[0].strip().equalsIgnoreCase("index")) {
			buildIndex();
		}
	}

	public static void buildIndex() throws Exception {
		System.out.println("Building package index.");
		PackageFetcher fetcher = new DubPackageFetcher();
		System.out.println("Fetching packages...");
		ArrayNode packagesArray = fetcher.fetch();
		int docCount = 0;
		Duration indexDuration;
		try (var indexer = new LucenePackageIndexer(Path.of("package-index"))) {
			Instant start = Instant.now();
			for (JsonNode node : packagesArray) {
				if (node.isObject()) {
					try {
						indexer.addToIndex((ObjectNode) node);
						docCount++;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			Instant end = Instant.now();
			indexDuration = Duration.between(start, end);
		}
		System.out.println("Done! Added " + docCount + " packages to the index in " + indexDuration.toMillis() + " ms.");
	}
}
