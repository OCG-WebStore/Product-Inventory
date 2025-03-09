FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY .. .
RUN ./gradlew stage

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/stage/main/lib /app/lib
COPY --from=builder /app/build/stage/main/conf /app/conf
COPY --from=builder /app/docker/product-inventory-scripts /app/product-inventory-scripts

RUN chmod +x /app/product-inventory-scripts/healthcheck.sh

ARG APP_VERSION
ENV VERSION=$APP_VERSION
ENV _JAVA_OPTIONS="-Dconfig.file=/app/conf/application.conf --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED"

EXPOSE 9000
CMD java -Dapp.version=${VERSION} -jar /app/lib/product-inventory-${VERSION}.jar