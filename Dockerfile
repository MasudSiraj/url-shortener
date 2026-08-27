# Multi-stage: build with JDK, run on JRE as non-root (task A3).
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
RUN apk add --no-cache maven
COPY pom.xml .
COPY config ./config
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests -Dspotless.check.skip -Dcheckstyle.skip package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && apk add --no-cache wget
USER app
WORKDIR /app
COPY --from=build /workspace/target/url-shortener-*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
