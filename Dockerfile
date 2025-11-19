# Multi-stage build for Spring Boot app with lib-version dependency
FROM maven:latest AS builder

WORKDIR /build

# Copy lib-version source and build it first
COPY lib-version ./lib-version
WORKDIR /build/lib-version
RUN mvn clean install -DskipTests

# Now build the main application
WORKDIR /build/app
COPY app/pom.xml .
COPY app/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:25-ea-jdk

WORKDIR /app


COPY --from=builder /build/app/target/*.jar app.jar

# F6 variables - Flexible container configuration
ENV SERVER_PORT=${SERVER_PORT:-8080}
ENV MODEL_HOST=${MODEL_HOST:-http://model-service:8081}


EXPOSE ${SERVER_PORT}

# Run the application
CMD ["java", "-jar", "app.jar"]
