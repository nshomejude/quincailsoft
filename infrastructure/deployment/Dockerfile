# Build the executable JAR with Java 25 and run verification before building this image.
# Supply an approved immutable Java 25 runtime image: registry/name@sha256:...
ARG JAVA_RUNTIME_IMAGE
FROM ${JAVA_RUNTIME_IMAGE}
WORKDIR /opt/quincaisoft
COPY --chown=10001:10001 backend/api-server/target/api-server-0.1.0-SNAPSHOT-exec.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+ExitOnOutOfMemoryError", "-jar", "/opt/quincaisoft/app.jar"]
