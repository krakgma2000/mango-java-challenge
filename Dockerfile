# Native image (GraalVM + Spring AOT), to quick start up and less memory ussage.
# I left the previous JVM version in Dockerfile.jvm (to compare)

# ---- Build ----
FROM ghcr.io/graalvm/native-image-community:21 AS build
# gradlew needs xargs
RUN microdnf install -y findutils && microdnf clean all
WORKDIR /workspace

# Gradle wrapper and build files first, so the dependencies are cached
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon test nativeCompile

# ---- Runtime ----
FROM debian:bookworm-slim
WORKDIR /app
COPY --from=build /workspace/build/native/nativeCompile/mango-app mango-app
EXPOSE 8080
ENTRYPOINT ["./mango-app"]
