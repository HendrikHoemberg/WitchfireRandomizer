FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline || true
COPY src/ src/
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as non-root user for security (explicit UID/GID 1000 for standard host mapping)
RUN userdel -r ubuntu 2>/dev/null || true; \
    groupadd -g 1000 spring && useradd -u 1000 -g spring -m spring
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs
USER spring:spring

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 9090

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_ADDRESS=0.0.0.0
ENV SERVER_PORT=9090
ENV LOG_PATH=/app/logs

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
