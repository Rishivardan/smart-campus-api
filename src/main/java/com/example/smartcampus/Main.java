package com.example.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;

public class Main {
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        final ResourceConfig config = new ResourceConfig()
                .packages("com.example.smartcampus")
                .register(JacksonJsonProvider.class);

        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) {
        HttpServer server = startServer();
        System.out.println("Smart Campus API running at " + BASE_URI);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
    }
}