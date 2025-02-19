FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY .. .
RUN ./gradlew stage

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/stage/main /app
COPY --from=builder /app/conf /app/conf
COPY --from=builder /app/docker/product-inventory-scripts /app/product-inventory-scripts

RUN chmod +x /app/bin/product-inventory &&\
    chmod +x /app/lib/product-inventory.jar &&\
    chmod +x /app/product-inventory-scripts/healthcheck.sh

ENV JAVA_OPTS="-Dconfig.file=/app/conf/application.conf --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED"

EXPOSE 9000
CMD ["/app/bin/product-inventory", "-jar", "/app/lib/product-inventory.jar"]