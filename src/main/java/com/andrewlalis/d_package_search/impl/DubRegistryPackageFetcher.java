package com.andrewlalis.d_package_search.impl;

import com.andrewlalis.d_package_search.PackageFetcher;
import com.andrewlalis.d_package_search.PackageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;

/**
 * A package fetcher that pulls directly from the Dub registry's JSON dump.
 */
public class DubRegistryPackageFetcher implements PackageFetcher {
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private static final String API_URL = "https://code.dlang.org/api/packages/dump";

	@Override
	public Collection<PackageInfo> fetch() throws IOException {
		HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
				.GET()
				.timeout(Duration.ofSeconds(60))
				.header("Accept", "application/json")
				.header("Accept-Encoding", "gzip")
				.build();
		try {
			HttpResponse<InputStream> response = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new IOException("Response status code " + response.statusCode());
			}
			ObjectMapper mapper = new ObjectMapper();
			try (var in = new GZIPInputStream(response.body())) {
				ArrayNode array = mapper.readValue(in, ArrayNode.class);
				Collection<PackageInfo> packages = new ArrayList<>();
				for (JsonNode node : array) {
					if (node.isObject()) {
						try {
							packages.add(parsePackage((ObjectNode) node));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				return packages;
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private PackageInfo parsePackage(ObjectNode obj) {
		return new PackageInfo(
				obj.get("name").asText(),
				mapJsonArray(obj.withArray("categories"), JsonNode::asText).toArray(new String[0]),
				mapJsonArray(obj.withArray("versions"), this::parseVersion).toArray(new PackageInfo.VersionInfo[0])
		);
	}

	private PackageInfo.VersionInfo parseVersion(JsonNode node) {
		String description = null;
		String license = null;
		String[] authors = new String[0];
		if (node.hasNonNull("info")) {
			JsonNode infoNode = node.get("info");
			if (infoNode.hasNonNull("description")) {
				description = infoNode.get("description").asText();
			}
			if (infoNode.hasNonNull("license")) {
				license = infoNode.get("license").asText();
			}
			if (infoNode.hasNonNull("authors")) {
				authors = mapJsonArray(infoNode.withArray("authors"), JsonNode::asText).toArray(authors);
			}
		}
		return new PackageInfo.VersionInfo(
				OffsetDateTime.parse(node.get("date").asText()).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
				node.get("version").asText(),
				description,
				license,
				authors,
				node.get("readme").asText()
		);
	}

	private static <T> List<T> mapJsonArray(ArrayNode array, Function<JsonNode, T> mapper) {
		List<T> list = new ArrayList<>(array.size());
		for (JsonNode node : array) {
			list.add(mapper.apply(node));
		}
		return list;
	}
}
