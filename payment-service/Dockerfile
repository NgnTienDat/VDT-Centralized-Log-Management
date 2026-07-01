FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Cài su-exec để hạ quyền từ root xuống appuser sau khi chown
RUN apk add --no-cache su-exec

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# KHÔNG set USER appuser ở đây nữa — entrypoint sẽ tự hạ quyền
EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]