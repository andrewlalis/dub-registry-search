package com.andrewlalis.d_package_search;

import java.time.LocalDateTime;

/**
 * Information about a D package that is ready for indexing.
 * @param name The name of the package.
 * @param categories The list of categories the package is in.
 * @param versions The known list of versions for this package.
 */
public record PackageInfo(
        String name,
        String[] categories,
        VersionInfo[] versions
) {
    /**
     * Information about a specific version of a D package.
     * @param timestamp The timestamp (in UTC) when the version was published.
     * @param versionTag The version tag string (e.g. "1.2.3").
     * @param description The version's description, or null.
     * @param license The version's license name (like "MIT" or "LGPL"), or null.
     * @param authors The list of authors for this version.
     * @param readmeText The text content of this version's README file.
     */
    public record VersionInfo(
            LocalDateTime timestamp,
            String versionTag,
            String description,
            String license,
            String[] authors,
            String readmeText
    ) {}
}
