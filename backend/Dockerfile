# syntax=docker/dockerfile:1
# Build context: ./backend    docker build -t quincaisoft/api backend
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY . .
RUN find . -name target -type d -prune -exec rm -rf {} + ; chmod +x mvnw
# Tests run in CI (mvn verify); the image build only packages.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -pl api-server -am -DskipTests package \
 && cp api-server/target/api-server-*-exec.jar /build/app.jar

FROM eclipse-temurin:25-jre AS runtime
RUN useradd --system --uid 10001 --no-create-home quincai
WORKDIR /srv
COPY --from=build /build/app.jar /srv/app.jar
USER 10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java","-jar","/srv/app.jar"]
