# Stage 1: Build Maven project
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .mvn/settings.xml ./

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s settings.xml dependency:go-offline


COPY src ./src

RUN --mount=type=secret,id=GITHUB_TOKEN \
    export GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) && \
    mvn -B -s settings.xml clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# F6 variables
ENV SERVER_PORT=${SERVER_PORT:-8080}
ENV MODEL_HOST=${MODEL_HOST:-http://model-service:8081}

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c"]
CMD ["java -jar app.jar --server.port=${SERVER_PORT}} --model.host=${MODEL_HOST}"]
