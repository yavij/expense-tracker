# Multi-stage build for Expense Tracker App

# Stage 1: Build frontend
FROM node:18-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy frontend source
COPY frontend/package*.json ./

# Install dependencies
RUN npm install

# Copy frontend source code
COPY frontend/ .

# Build frontend
RUN npm run build


# Stage 2: Build backend
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app/backend

# Copy pom.xml
COPY backend/pom.xml .

# Download dependencies
RUN mvn dependency:resolve

# Copy backend source code
COPY backend/ .

# Build backend (skip tests for faster build)
RUN mvn clean package -DskipTests


# Stage 3: Runtime
FROM eclipse-temurin:17-jre-slim

WORKDIR /app

# Set Java memory options
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Copy the compiled JAR from backend build stage
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# Copy the built frontend from frontend build stage
COPY --from=frontend-builder /app/frontend/dist ./public

# Expose port 7000
EXPOSE 7000

# Install curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:7000/health || exit 1

# Run the application (shell form to expand $JAVA_OPTS)
CMD java $JAVA_OPTS -jar app.jar
