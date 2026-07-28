package com.example.systemtest;

import com.example.impl.Server;
import com.example.xml.model.ObjectFactory;
import com.example.xml.model.OrderMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Systemtest fuer den kompletten Stack:
 *
 *   XSD-generierte JAXB-Klasse (xml-model)
 *     -> JAX-RS-Endpoint (generiertes Interface aus api, implementiert in impl)
 *     -> echter, eingebetteter HTTP-Server
 *     -> echte HTTP-Anfrage mit XML-Body
 *     -> echte HTTP-Antwort, geparst per JAXB
 *
 * Es wird bewusst NICHTS gemockt: der Test startet den echten
 * Server-Prozess (im selben JVM, auf einem freien Port) und spricht ihn
 * ueber java.net.http.HttpClient an, so wie es ein echter Client tun wuerde.
 *
 * Hinweis: Falls das generierte OrdersApi-Interface (siehe
 * api/target/generated-sources/openapi/src/gen/java/com/example/api/OrdersApi.java)
 * einen anderen Pfad als "/orders" verwendet, bitte die URI unten anpassen.
 */
class OrderServiceSystemIT {

    private static HttpServer server;
    private static String baseUri;
    private static JAXBContext jaxbContext;
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    @BeforeAll
    static void startServer() throws Exception {
        int port = findFreePort();
        baseUri = "http://localhost:" + port + "/";
        server = Server.startServer(baseUri);
        jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void createOrder_liefertDieselbeBestellungAlsXmlZurueck() throws Exception {
        OrderMessage request = new OrderMessage();
        request.setOrderId("ORD-1");
        request.setCustomerName("Max Mustermann");
        request.setAmount(new BigDecimal("99.90"));

        String requestXml = marshal(request);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + "orders"))
                .header("Content-Type", "application/xml")
                .header("Accept", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(requestXml, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Unerwarteter Status. Body war: " + response.body());

        OrderMessage responseOrder = unmarshal(response.body());
        assertEquals(request.getOrderId(), responseOrder.getOrderId());
        assertEquals(request.getCustomerName(), responseOrder.getCustomerName());
        assertEquals(0, request.getAmount().compareTo(responseOrder.getAmount()));
    }

    private static String marshal(OrderMessage order) throws Exception {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(OBJECT_FACTORY.createOrderMessage(order), out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static OrderMessage unmarshal(String xml) throws Exception {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object result = unmarshaller.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        if (result instanceof JAXBElement) {
            return (OrderMessage) ((JAXBElement<?>) result).getValue();
        }
        return (OrderMessage) result;
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
