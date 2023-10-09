package com.andrewlalis.d_package_search;

import java.io.IOException;
import java.util.Collection;

/**
 * A component responsible for fetching up-to-date information about packages.
 */
public interface PackageFetcher {
	Collection<PackageInfo> fetch() throws IOException;
}
