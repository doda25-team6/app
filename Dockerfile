# Multi-stage build for Spring Boot app with lib-version dependency
FROM maven:latest AS builder

WORKDIR /app

<<<<<<< HEAD
# F2: Configure Maven settings for GitHub Package Registry
RUN mkdir -p /root/.m2
COPY app/settings.xml /root/.m2/settings.xml

# F2: build with released lib-version from GitHub Package Registry
WORKDIR /build/app
COPY app/pom.xml .

# Set build arguments for GitHub authentication
ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
ENV GITHUB_USERNAME=${GITHUB_USERNAME}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

# F2: Resolve dependencies from GitHub Package Registry
RUN mvn dependency:go-offline -B

WORKDIR /build/app
COPY app/src ./src
RUN mvn clean package -DskipTests
=======
COPY pom.xml .mvn/settings.xml ./

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s settings.xml dependency:go-offline

>>>>>>> 31ea35d (feat: use secret token to fetch lib-version)

COPY src ./src

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s settings.xml clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/app/target/*.jar app.jar

# F6 variables - Flexible container configuration
ENV SERVER_PORT=${SERVER_PORT:-8080}
ENV MODEL_HOST=${MODEL_HOST:-http://model-service:8081}

# Expose the configurable port
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c"]
CMD ["java -jar app.jar --server.port=${SERVER_PORT} --model.host=${MODEL_HOST}"]
