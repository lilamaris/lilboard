# Build from the repository root:
# docker build -f board-service/launcher/Dockerfile -t lilboard-board:local .
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew \
    && ./gradlew :board-service:launcher:bootJar --no-daemon --console=plain \
    && cp board-service/launcher/build/libs/*-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
LABEL org.opencontainers.image.title="lilboard board-service" \
    org.opencontainers.image.source="https://github.com/lilamaris/lilboard"
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/app.jar ./app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
