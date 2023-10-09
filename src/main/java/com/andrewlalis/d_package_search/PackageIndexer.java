package com.andrewlalis.d_package_search;

/**
 * A package indexer writes information from a given JSON package object to an
 * index for searching later.
 */
public interface PackageIndexer extends AutoCloseable {
	void addToIndex(PackageInfo info) throws Exception;

	@Override
	default void close() throws Exception {}
}
