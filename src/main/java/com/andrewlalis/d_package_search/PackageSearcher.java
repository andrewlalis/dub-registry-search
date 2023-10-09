package com.andrewlalis.d_package_search;

import java.util.SequencedCollection;

public interface PackageSearcher {
    SequencedCollection<PackageSearchResult> search(String query);
}
