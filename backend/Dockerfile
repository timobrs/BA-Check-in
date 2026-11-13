FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system bacheckin \
    && useradd --system --gid bacheckin --home-dir /app bacheckin

COPY backend/build/libs/backend.jar /app/backend.jar

USER bacheckin

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
