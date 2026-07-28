package com.example.impl;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

/**
 * Startet einen eingebetteten HTTP-Server fuer die Beispiel-Implementierung.
 * Kein Application-Server noetig - nuetzlich lokal, im Systemtest und
 * als Ausgangspunkt fuer eine echte Deployment-Variante (WAR/Quarkus/...).
 */
public final class Server {

    public static final String DEFAULT_BASE_URI = "http://localhost:8080/";

    private Server() {
    }

    /**
     * Startet den Server auf der uebergebenen Basis-URI (z.B. mit
     * dynamischem Port fuer Tests) und registriert alle Ressourcen.
     */
    public static HttpServer startServer(String baseUri) {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(OrdersResourceImpl.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), resourceConfig);
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer(DEFAULT_BASE_URI);
        System.out.println("Server gestartet unter " + DEFAULT_BASE_URI + " - Enter zum Beenden.");
        System.in.read();
        server.shutdownNow();
    }
}
