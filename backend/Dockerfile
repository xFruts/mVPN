FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .

RUN mvn dependency:resolve -DoutputFile=/dev/null

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

ARG VERSION=0.0.1

LABEL maintainer="ru.maxow" \
      version="${VERSION}" \
      description="mVPN Backend Service"

RUN addgroup --system --gid 1001 javauser && \
    adduser --system --uid 1001 --ingroup javauser --shell /bin/false javauser

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

RUN chown javauser:javauser app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SERVER_PORT=8080

USER javauser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]