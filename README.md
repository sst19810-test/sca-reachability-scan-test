# Java Maven method usage demo

Requires JDK 17+ and Maven 3.6.3+. This local learning/scanner fixture deliberately pins the dependency coordinates supplied in the attachment: Jackson Databind 2.18.0 and PostgreSQL JDBC 42.7.4. These are historical versions associated with the supplied vulnerability report; do not use this fixture as a production dependency baseline.

## Run

From this directory:

```sh
mvn clean test
mvn compile exec:java
mvn compile exec:java -Dexec.args=single
mvn compile exec:java -Dexec.args=cross
```

The default runs both implementations. No PostgreSQL server, credentials, network calls, custom filesystem provider, or external input is required at runtime. Maven initially downloads dependencies. The program uses a local file URI, a short XML duration, one fallback event, and four padding bytes in a new in-memory buffer. It never opens the file path.

## Selected methods and call paths

| Method from supplied report | Single-file application path | Cross-file application path | Invocation |
|---|---|---|---|
| `com.fasterxml.jackson.databind.ext.NioPathDeserializer.deserialize` | `SingleFileExamples.readPath` → `NioPathDeserializer.deserialize` | `CrossFileExamples.readPath` → `JsonGateway.deserializePath` → `NioPathDeserializer.deserialize` | Direct invocation with a positioned parser and initialized context |
| `com.fasterxml.jackson.databind.ext.CoreXMLDeserializers.Std._deserialize` | `SingleFileExamples.readDuration` → `ObjectMapper.readValue` | `CrossFileExamples.readDuration` → `JsonGateway.read` → `ObjectMapper.readValue` | Jackson dispatches to the XML Duration deserializer |
| `com.fasterxml.jackson.databind.jsontype.impl.TypeDeserializerBase._findDeserializer` | `SingleFileExamples.readEvent` → `ObjectMapper.readValue`, using nested `Event` | `CrossFileExamples.readEvent` → `JsonGateway.read`, using `Event.java` | NAME polymorphism selects the default implementation |
| `org.postgresql.util.internal.PgBufferedOutputStream.writeZeros` | `SingleFileExamples.writePadding` → `writeZeros` | `CrossFileExamples.writePadding` → `BufferGateway.writePadding` → `writeZeros` | Direct driver method call |

The Path examples directly invoke `new NioPathDeserializer().deserialize(parser, context)` after advancing the parser to its first token and creating a configured per-call `DefaultDeserializationContext`. The XML and polymorphic examples reach their library hooks through `ObjectMapper.readValue`. No library implementations are copied, mocked, or called through reflection. The PostgreSQL method is public but belongs to an internal package and may change between versions. Single-file means the application logic and nested model reside in one source file; third-party library code still lives in dependencies. Cross-file means the input passes between application classes in separate files before reaching the dependency.

This is method-reachability demonstration, not verification or reproduction of the CVE claims in the attachment. It selects four methods rather than covering every entry. Jackson 3 `tools.jackson.*` methods are excluded because the supplied dependency is Jackson 2. Tests check observable behavior for both layouts; they do not instrument library method entry or attempt resource exhaustion, arbitrary class loading, or stale-buffer disclosure.

## Files

- `src/main/java/demo/Main.java`: runs either or both layouts.
- `src/main/java/demo/single/SingleFileExamples.java`: all four same-file examples.
- `src/main/java/demo/cross/CrossFileExamples.java`: cross-file service entry points.
- `src/main/java/demo/cross/JsonGateway.java`: Jackson call site.
- `src/main/java/demo/cross/Event.java`: separately declared polymorphic model.
- `src/main/java/demo/cross/BufferGateway.java`: PostgreSQL call site.
- `src/test/java/demo/ExamplesTest.java`: behavior checks for both layouts.

For manual runtime confirmation, set method breakpoints on the selected library methods in your IDE, attach Maven dependency sources, and run `demo.Main`.

Sources: [Jackson source at 2.18.0](https://github.com/FasterXML/jackson-databind/tree/jackson-databind-2.18.0/src/main/java/com/fasterxml/jackson/databind), [pgJDBC source at 42.7.4](https://github.com/pgjdbc/pgjdbc/tree/REL42.7.4/pgjdbc/src/main/java/org/postgresql), [pgJDBC release notes](https://github.com/pgjdbc/pgjdbc/blob/master/CHANGELOG.md).
