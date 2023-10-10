package com.andrewlalis.d_package_search;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

/**
 * The index generator is a component that pieces all the parts of building an
 * index together into one runnable. It fetches packages using a fetcher, then
 * indexes them using an indexer obtained from the given supplier.
 * @param fetcher The fetcher to use to get packages.
 * @param indexerSupplier A supplier for a package indexer.
 */
public record IndexGenerator(
        PackageFetcher fetcher,
        ThrowableSupplier<PackageIndexer> indexerSupplier
) implements Runnable {
    @Override
    public void run() {
        System.out.println("Generating index...");
        Instant start;
        Duration dur;
        start = Instant.now();
        Collection<PackageInfo> packages;
        try {
            packages = fetcher.fetch();
            dur = Duration.between(start, Instant.now());
            System.out.println("Fetched " + packages.size() + " in " + dur.toMillis() + " ms.");
        } catch (IOException e) {
            System.err.println("Failed to fetch packages: " + e.getMessage());
            return;
        }

        try (PackageIndexer indexer = indexerSupplier.get()) {
            start = Instant.now();
            for (var pkg : packages) {
                indexer.addToIndex(pkg);
            }
            dur = Duration.between(start, Instant.now());
            System.out.println("Indexed all packages in " + dur.toMillis() + " ms.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
