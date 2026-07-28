package com.example.impl;

import com.example.api.OrdersApi;
import com.example.xml.model.ObjectFactory;
import com.example.xml.model.OrderMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
 *
 * Die @Path/@POST/@Consumes/@Produces-Annotationen sind laut JAX-RS-Spec
 * zwar vom Interface vererbbar, werden hier aber zusaetzlich explizit auf
 * die Implementierung gesetzt - das ist robuster gegenueber Unterschieden
 * zwischen JAX-RS-Runtimes/-Versionen bei der Annotations-Vererbung von
 * Interfaces.
 *
 * Wichtig fuer XML/JAXB: die generierte Klasse OrderMessage traegt (aus der
 * XSD) kein @XmlRootElement, sondern nur @XmlType. Lesen (Unmarshalling des
 * Request-Bodys in den Methodenparameter) funktioniert damit trotzdem, weil
 * JAX-RS/JAXB dabei intern unmarshal(source, OrderMessage.class) nutzt.
 * Schreiben (Marshalling der Response) braucht dagegen zwingend einen
 * Root-Element-Namen - den liefert hier die generierte ObjectFactory ueber
 * JAXBElement<OrderMessage>. Ohne dieses Wrapping meldet Jersey:
 * "MessageBodyWriter not found for media type=application/xml, type=class
 * ...OrderMessage".
 */
@Path("/orders")
public class OrdersResourceImpl implements OrdersApi {
    
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    
    @Override
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
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
