package io.github.andrewlalis.dub_registry_search;

import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;

public interface PackageFetcher {
	ArrayNode fetch() throws IOException;
}
