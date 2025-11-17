# Stage 1: Build Maven project
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests -Dmaven.compiler.release=17 clean package

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# F6 variables
ENV SERVER_PORT=${SERVER_PORT:-8080}
ENV MODEL_HOST=${MODEL_HOST:-http://model-service:8081}

EXPOSE ${SERVER_PORT}

CMD ["java", "-jar", "app.jar"]
