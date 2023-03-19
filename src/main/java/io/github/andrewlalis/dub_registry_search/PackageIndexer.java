package io.github.andrewlalis.dub_registry_search;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public interface PackageIndexer {
	void addToIndex(ObjectNode packageJson) throws IOException;
}
