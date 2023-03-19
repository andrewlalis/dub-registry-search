package io.github.andrewlalis.dub_registry_search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.GZIPInputStream;

public class DubPackageFetcher implements PackageFetcher {
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(3))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	private static final String API_URL = "https://code.dlang.org/api/packages/dump";

	@Override
	public ArrayNode fetch() throws IOException {
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
				return mapper.readValue(in, ArrayNode.class);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
