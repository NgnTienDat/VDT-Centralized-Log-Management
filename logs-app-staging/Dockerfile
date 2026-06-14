# ================================
# Stage 1: Build
# ================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml trước để tận dụng layer cache của Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code và build
COPY src ./src
RUN mvn package -DskipTests -B

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Tạo user non-root để chạy app (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Tạo thư mục logs và phân quyền cho appuser
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

USER appuser

# Spring Boot mặc định port 8081
EXPOSE 8081

# Chạy app; logs sẽ được ghi vào /app/logs (mount từ host qua compose)
ENTRYPOINT ["java", "-jar", "app.jar"]