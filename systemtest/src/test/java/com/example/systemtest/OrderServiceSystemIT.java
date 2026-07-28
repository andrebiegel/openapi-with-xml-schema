package com.example.systemtest;

import com.example.impl.Server;
import com.example.xml.model.OrderMessage;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Systemtest fuer den kompletten Stack (javax-/Java-8-konforme Variante):
 *
 *   XSD-generierte JAXB-Klasse (xml-model, javax.xml.bind, mit @XmlRootElement)
 *     -> JAX-RS-Endpoint (generiertes Interface aus api, javax.ws.rs, implementiert in impl)
 *     -> echter, eingebetteter HTTP-Server (Jersey 2.x + Grizzly)
 *     -> echte HTTP-Anfrage mit XML-Body
 *     -> echte HTTP-Antwort, geparst per JAXB
 *
 * Es wird bewusst NICHTS gemockt: der Test startet den echten
 * Server-Prozess (im selben JVM, auf einem freien Port) und spricht ihn
 * ueber java.net.HttpURLConnection an (statt java.net.http.HttpClient,
 * das erst ab Java 11 verfuegbar ist - damit bleibt der Test selbst
 * ebenfalls Java-8-kompatibel).
 *
 * Da OrderMessage jetzt @XmlRootElement traegt (anonymer Complex-Type in der
 * XSD), reicht direktes Marshalling/Unmarshalling von OrderMessage ohne
 * ObjectFactory/JAXBElement-Wrapping.
 *
 * Hinweis: Falls das generierte OrdersApi-Interface (siehe
 * api/target/generated-sources/openapi/src/gen/java/com/example/api/OrdersApi.java)
 * einen anderen Pfad als "/orders" verwendet, bitte die URI unten anpassen.
 */
class OrderServiceSystemIT {
    
    private static HttpServer server;
    private static String baseUri;
    private static JAXBContext jaxbContext;
    
    @BeforeAll
    static void startServer() throws Exception {
        int port = findFreePort();
        baseUri = "http://localhost:" + port + "/";
        server = Server.startServer(baseUri);
        jaxbContext = JAXBContext.newInstance(OrderMessage.class);
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
        String responseXml = postXml(baseUri + "orders", requestXml);
        
        OrderMessage responseOrder = unmarshal(responseXml);
        assertEquals(request.getOrderId(), responseOrder.getOrderId());
        assertEquals(request.getCustomerName(), responseOrder.getCustomerName());
        assertEquals(0, request.getAmount().compareTo(responseOrder.getAmount()));
    }
    
    /**
     * Sendet einen XML-Body per POST via HttpURLConnection (Java 8, kein
     * zusaetzlicher HTTP-Client noetig).
     *
     * WICHTIG: prueft den Status BEVOR ein Stream gelesen wird. Bei einem
     * Fehlerstatus (nicht 2xx) kann getErrorStream() laut Javadoc durchaus
     * null liefern (z.B. wenn der Server keinen Body gesendet hat) - das
     * ist der typische Ausloeser fuer eine NullPointerException, wenn man
     * blind darauf liest. Stattdessen wird hier eine aussagekraeftige
     * Fehlermeldung mit Status, Headern und Body (falls vorhanden) erzeugt.
     */
    private static String postXml(String url, String xmlBody) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/xml");
            
            byte[] bytes = xmlBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
            
            int status;
            try {
                status = connection.getResponseCode();
            } catch (IOException e) {
                String errorBody = readFully(connection.getErrorStream());
                fail("Verbindung zu " + url + " fehlgeschlagen: " + e.getMessage()
                        + " Error-Body: [" + errorBody + "]");
                throw e; // unreachable, nur fuer den Compiler
            }
            
            if (status < 200 || status >= 300) {
                String errorBody = readFully(connection.getErrorStream());
                fail("Unerwarteter Status " + status + " von " + url
                        + ". Response-Header: " + connection.getHeaderFields()
                        + " Body: [" + errorBody + "]");
            }
            
            return readFully(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }
    
    private static String readFully(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString("UTF-8");
    }
    
    private static String marshal(OrderMessage order) throws Exception {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(order, out);
        return out.toString("UTF-8");
    }
    
    private static OrderMessage unmarshal(String xml) throws Exception {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (OrderMessage) unmarshaller.unmarshal(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    
    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
