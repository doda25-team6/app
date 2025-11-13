# SMS Checker - App Service

**Frontend web application and API gateway for SMS spam detection**

This Spring Boot application serves as both the web frontend and API gateway for the SMS Checker system. It provides a web interface for users to submit SMS messages and get spam classification results from the model service.

## Features

- Web-based UI for SMS spam detection
- API gateway to prevent CORS issues
- Integration with [lib-version](https://github.com/doda25-team12/lib-version) library
- REST client for model-service communication

## Requirements

- Java 25+
- Maven 3.6+
- Access to [model-service](https://github.com/doda25-team12/model-service) running on configured host

## Configuration

Set the `MODEL_HOST` environment variable to specify where the model service is running:

```bash
export MODEL_HOST="http://localhost:8081"
```

## Running

### With Maven
```bash
MODEL_HOST="http://localhost:8081" mvn spring-boot:run
```

### With Docker
```bash
docker build -t sms-checker-app .
docker run -p 8080:8080 -e MODEL_HOST="http://model-service:8081" sms-checker-app
```

## Usage

Once running, access the application at:
- **Web UI**: http://localhost:8080/sms
- **Health Check**: http://localhost:8080/actuator/health

## Architecture

This service acts as an API gateway, receiving requests from the web frontend and forwarding them to the model-service. This pattern prevents CORS issues and provides a clean separation between the UI and ML components.

## Dependencies

- **lib-version**: Version-aware library for version management
- **model-service**: Backend ML service for spam classification

See the [operation repository](https://github.com/doda25-team12/operation) for complete system documentation.


