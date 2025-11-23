# Multi-stage build for Spring Boot app with lib-version dependency
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# F2: Auth required for GitHub Package Registry
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN

# To make them available to Maven (used by settings.xml)
ENV GITHUB_USERNAME=${GITHUB_USERNAME}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

COPY pom.xml .
COPY .mvn/settings.xml .mvn/settings.xml

# Download dependencies
RUN mvn -B -s .mvn/settings.xml dependency:go-offline

# RUN --mount=type=secret,id=GITHUB_TOKEN \
#     export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
#     mvn -B -s .mvn/settings.xml dependency:go-offline


COPY src ./src

RUN mvn -B -s .mvn/settings.xml clean package -DskipTests

# RUN --mount=type=secret,id=GITHUB_TOKEN \
#     export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
#     mvn -B -s .mvn/settings.xml clean package -DskipTests


# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# F6 variables - Flexible container configuration
ENV SERVER_PORT=8080
ENV MODEL_HOST=http://model-service:8081

# Expose the configurable port
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c"]
CMD ["java -jar app.jar --server.port=${SERVER_PORT} --model.host=${MODEL_HOST}"]
