FROM eclipse-temurin:17 AS builder

WORKDIR /src
COPY . .
RUN ./gradlew build

# Final image
FROM ontotext/graphdb:10.2.1

# Copy JAR plugin
COPY --from=builder /src/build/libs/ /opt/graphdb/dist/lib/plugins/compound-naming/

# Copy data.ttl and enable server files import in the workbench
COPY --from=builder /src/src/main/resources/data.ttl /opt/graphdb/home/graphdb-import/data.ttl
ENV GDB_JAVA_OPTS="-Dgraphdb.workbench.importDirectory=/opt/graphdb/home/graphdb-import"
