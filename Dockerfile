# Multi-stage build for Spring Boot app with lib-version dependency
FROM maven:latest AS builder

WORKDIR /app

COPY pom.xml .mvn/settings.xml ./

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s settings.xml dependency:go-offline


COPY src ./src

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s settings.xml clean package -DskipTests

# Now build the main application
WORKDIR /build/app
COPY app/pom.xml .
COPY app/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:25-ea-jdk

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/app/target/*.jar app.jar

# F6 variables - Flexible container configuration
ENV SERVER_PORT=${SERVER_PORT:-8080}
ENV MODEL_HOST=${MODEL_HOST:-http://model-service:8081}

# Expose the configurable port
EXPOSE ${SERVER_PORT}

CMD ["java -jar app.jar --server.port=${SERVER_PORT} --model.host=${MODEL_HOST}"]
