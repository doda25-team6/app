# Multi-stage build for Spring Boot app with lib-version dependency
FROM maven:latest AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn/settings.xml .mvn/settings.xml

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s .mvn/settings.xml dependency:go-offline


COPY src ./src

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s .mvn/settings.xml clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/app/target/*.jar app.jar

# F6 variables
ENV SERVER_PORT=8080
ENV MODEL_HOST=http://model-service:8081

# Expose the configurable port
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c"]
CMD ["java -jar app.jar --server.port=${SERVER_PORT} --model.host=${MODEL_HOST}"]
