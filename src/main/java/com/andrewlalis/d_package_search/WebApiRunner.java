package com.andrewlalis.d_package_search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.SequencedCollection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Component that runs a simple HTTP endpoint, defaulting to localhost:8080/search?query=...
 * that allows clients to search the index via an HTTP request.
 */
public final class WebApiRunner extends Handler.Abstract implements Runnable {
    private final PackageSearcher packageSearcher;
    private final ObjectMapper objectMapper;
    private final Executor threadPoolExecutor;

    public WebApiRunner(PackageSearcher packageSearcher) {
        this.packageSearcher = packageSearcher;
        this.objectMapper = new ObjectMapper();
        this.threadPoolExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void run() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setVirtualThreadsExecutor(threadPoolExecutor);
        threadPool.setName("http-server");
        Server server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(DPackageSearch.getIntProp("server.port", 8080));
        connector.setHost(DPackageSearch.getStringProp("server.host"));
        server.addConnector(connector);
        server.setHandler(this);
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        if (request.getMethod().equalsIgnoreCase("GET")) {
            HttpURI uri = request.getHttpURI();
            if (uri.getPath().equalsIgnoreCase("/search")) {
                String query = uri.getQuery() == null ? null : parseQuery(uri);
                if (query == null || query.isBlank()) {
                    response.setStatus(HttpStatus.BAD_REQUEST_400);
                    response.write(true, ByteBuffer.wrap("Missing required \"query\" parameter.".getBytes(StandardCharsets.UTF_8)), callback);
                } else {
                    System.out.println("Searching with query \"" + query + "\".");
                    SequencedCollection<PackageSearchResult> results = packageSearcher.search(query);
                    response.setStatus(HttpStatus.OK_200);
                    response.getHeaders().add("Content-Type", "application/json; charset=utf-8");
                    byte[] responseBody = objectMapper.writeValueAsBytes(results);
                    response.write(true, ByteBuffer.wrap(responseBody), callback);
                }
            } else if (uri.getPath().equalsIgnoreCase("/index.html") || uri.getPath().equalsIgnoreCase("/")) {
                try (var in = WebApiRunner.class.getClassLoader().getResourceAsStream("index.html")) {
                    if (in == null) throw new IOException("Resource doesn't exist.");
                    response.setStatus(HttpStatus.OK_200);
                    response.getHeaders().add("Content-Type", "text/html; charset=utf-8");
                    response.write(true, ByteBuffer.wrap(in.readAllBytes()), callback);
                }
            } else {
                response.setStatus(HttpStatus.NOT_FOUND_404);
            }
        } else {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405);
        }
        callback.succeeded();
        return true;
    }

    private static String parseQuery(HttpURI uri) {
        for (String pair : URLDecoder.decode(uri.getQuery(), StandardCharsets.UTF_8).split("&")) {
            int idx = pair.indexOf('=');
            if (idx != -1) {
                String key = pair.substring(0, idx);
                if (key.trim().equalsIgnoreCase("query")) {
                    return pair.substring(idx + 1).trim().toLowerCase();
                }
            }
        }
        return null;
    }
}
