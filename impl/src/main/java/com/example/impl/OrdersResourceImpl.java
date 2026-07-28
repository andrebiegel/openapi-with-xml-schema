package com.example.impl;

import com.example.api.OrdersApi;
import com.example.xml.model.OrderMessage;
import jakarta.ws.rs.core.Response;

/**
 * Beispiel-Implementierung des von openapi-generator (generatorName=jaxrs-spec)
 * aus api.yaml generierten Interfaces.
 *
 * WICHTIG: Der genaue Interface-Name und die genaue Methodensignatur haengen
 * von der jeweiligen openapi-generator-Version und den configOptions ab.
 * Nach dem ersten "mvn generate-sources" im api-Modul befindet sich das
 * generierte Interface unter:
 *   api/target/generated-sources/openapi/src/gen/java/com/example/api/OrdersApi.java
 * Bitte gegen diese Datei abgleichen und diese Klasse ggf. anpassen
 * (z.B. falls das Interface statt Response direkt OrderMessage zurueckgibt,
 * oder eine "throws" Klausel/andere Annotationen erwartet).
 */
public class OrdersResourceImpl implements OrdersApi {

    @Override
    public OrderMessage createOrder(OrderMessage orderMessage) {
        // Hier wuerde die eigentliche Geschaeftslogik stehen.
        // Als Beispiel wird die empfangene Bestellung einfach wieder
        // zurueckgegeben (Echo), um den kompletten Marshalling-Roundtrip
        // (XML -> JAXB-Objekt -> Verarbeitung -> JAXB-Objekt -> XML) zu zeigen.
        System.out.println("Order empfangen: orderId=" + orderMessage.getOrderId()
                + ", customerName=" + orderMessage.getCustomerName());

        return orderMessage;
    }
}
