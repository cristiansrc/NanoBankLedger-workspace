# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S nanobank && adduser -S nanobank -G nanobank
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R nanobank:nanobank /app
USER nanobank
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
