FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests && \
    mv target/*.jar /build/app.jar

FROM eclipse-temurin:21-jre-jammy

ARG VERSION=0.0.1
ARG BUILD_DATE
ARG VCS_REF

LABEL maintainer="ru.maxow" \
      version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.title="mVPN Backend Service" \
      org.opencontainers.image.description="Spring Boot backend with mID, Kafka, MinIO integration"

# Нужен только для HEALTHCHECK (curl) + корректные таймзоны.
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1001 javauser && \
    useradd --system --uid 1001 --gid javauser --shell /bin/false --home-dir /app javauser

WORKDIR /app
COPY --from=builder /build/app.jar /app/app.jar

RUN chown -R javauser:javauser /app

USER javauser

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Если actuator не подключён, healthcheck будет фейлиться — это ок, compose-уровень можно отключить.
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
