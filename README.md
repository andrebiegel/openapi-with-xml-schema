# order-service-parent

## Variante: javax / Java 8

Dieses Projekt ist auf die **javax-Namensraeume** (JAXB 2.3.x `javax.xml.bind`,
JAX-RS 2.1 `javax.ws.rs`, Jersey 2.x) und **Java 8** als Ziel-Bytecode-Level
(`maven.compiler.source/target=1.8`) ausgerichtet - bewusst NICHT die
Jakarta-EE-9+-Umbenennung (`jakarta.*`).

Wichtige Konsequenz: `openapi-generator-maven-plugin` 7.x selbst benoetigt
zum **Ausfuehren** (also fuer den Build) ein JDK 11+, erzeugt dabei aber
Code, der mit `javax.ws.rs` arbeitet und mit `source/target=1.8` kompiliert
- also auf einer echten Java-8-Laufzeitumgebung lauffaehig ist. Wenn auch
der Build selbst zwingend mit einem Java-8-JDK laufen muss, braucht ihr
eine deutlich aeltere openapi-generator-Version (4.x/5.x).

Multi-Modul-Maven-Projekt, das zwei Codegenerierungs-Schritte kombiniert:

1. **`xml-model`**: generiert per JAXB (`jaxb-maven-plugin`, xjc) Java-Klassen
   aus der XSD unter `xml-model/src/main/resources/xsd/order.xsd`.
2. **`api`**: generiert per `openapi-generator-maven-plugin` JAX-RS-Interfaces
   (Generator `jaxrs-spec`) aus `api/src/main/resources/openapi/api.yaml`.
   Für den XML-Payload wird **keine** eigene Modellklasse generiert, sondern
   über `importMappings`/`schemaMappings` direkt auf die in `xml-model`
   erzeugte JAXB-Klasse `com.example.xml.model.OrderMessage` verwiesen.

## Warum zwei Module?

Wenn man alles in einem Modul macht, kann es passieren, dass der
openapi-generator für dasselbe Schema eine eigene (Jackson-basierte)
Modellklasse erzeugt, die mit der JAXB-Klasse aus der XSD kollidiert oder
inkompatibel ist. Durch die Trennung:

- ist die XSD die **einzige Quelle der Wahrheit** für die XML-Struktur,
- wird diese eine Klasse sowohl im Server (JAX-RS-Endpoint) als auch überall
  sonst wiederverwendet,
- verhindert man doppelte/konkurrierende Modelle.

Das `api`-Modul hat eine Maven-Abhängigkeit auf `xml-model`, daher baut der
Reactor `xml-model` automatisch zuerst.

Zusaetzlich:

3. **`impl`**: Beispiel-Implementierung des generierten JAX-RS-Interfaces
   `OrdersApi`, lauffaehig als eingebetteter HTTP-Server
   (Jersey + Grizzly, kein Application-Server noetig).
4. **`systemtest`**: echter Ende-zu-Ende-Systemtest (`maven-failsafe-plugin`,
   `*IT`-Klassen), der den Server aus `impl` startet, eine XML-Anfrage per
   HTTP schickt und die XML-Antwort per JAXB prueft - ohne Mocking.

## Bauen

```bash
mvn -f order-service-parent clean install
```

Generierte Sourcen liegen danach unter:

- `xml-model/target/generated-sources/jaxb/com/example/xml/model/OrderMessage.java`
- `api/target/generated-sources/openapi/src/gen/java/com/example/api/...`

## Beispiel-Implementierung starten

```bash
mvn -f order-service-parent -pl impl -am package
java -cp impl/target/classes:$(find ~/.m2 -name '*.jar' | tr '\n' ':') com.example.impl.Server
```
(oder einfacher: `mvn -pl impl exec:java -Dexec.mainClass=com.example.impl.Server`,
falls das `exec-maven-plugin` ergaenzt wird). Danach ist der Endpunkt unter
`http://localhost:8080/orders` erreichbar.

## Systemtest ausfuehren

```bash
mvn -f order-service-parent verify -pl systemtest -am
```

`-am` sorgt dafuer, dass zuvor alle Module gebaut werden, von denen
`systemtest` abhaengt (`xml-model`, `api`, `impl`). Der Test startet den
Server selbst auf einem freien Port, schickt echtes XML per HTTP-POST an
`/orders` und vergleicht die Antwort.

## Wichtiger Hinweis zur Verifikation

Dieses Projekt wurde als Scaffold erstellt, ohne lokal gegen Maven Central
kompiliert zu werden. Nach dem ersten `mvn generate-sources` im `api`-Modul
unbedingt die tatsaechlich generierte Datei
`api/target/generated-sources/openapi/src/gen/java/com/example/api/OrdersApi.java`
mit `OrdersResourceImpl` abgleichen, insbesondere:

- exakter Interface-/Methodenname (haengt vom Tag `Orders` im OpenAPI-Dokument ab),
- Rueckgabetyp (`Response` vs. direkt `OrderMessage`),
- Pfad (`@Path`) auf Klassen- und Methodenebene,
- ob `OrderMessage` von xjc automatisch `@XmlRootElement` erhaelt oder ob
  im JAX-RS-Layer explizit ueber `ObjectFactory`/`JAXBElement` gewrappt
  werden muss (im Systemtest wird bereits defensiv mit `ObjectFactory`
  gearbeitet).

## Anpassen an eure echte XSD/OpenAPI

- Eigene XSD(s) unter `xml-model/src/main/resources/xsd/` ablegen
  (Package ggf. über `generatePackage` im `xml-model/pom.xml` anpassen).
- `api/src/main/resources/openapi/api.yaml` durch eure echte Spec ersetzen.
- Für jedes Schema, das eigentlich XML/XSD-Typen entspricht, in
  `importMappings`/`schemaMappings` einen Eintrag `SchemaName=voll.qualifizierter.Klassenname`
  ergänzen. Alle anderen (rein JSON-basierten) Schemas werden normal
  generiert.
- Falls JAX-RS mit JAXB direkt (statt Jackson-XML) marshallen soll, achtet
  darauf, dass ein `Jaxb2Provider`/`MOXyJsonProvider`-Äquivalent bzw. die
  Standard-JAXB-Unterstützung eures JAX-RS-Runtime (z. B. RESTEasy, Jersey)
  aktiviert ist – die generierten JAXB-Klassen sind bereits mit
  `@XmlRootElement`/`@XmlType` annotiert und sollten „out of the box“
  funktionieren.
