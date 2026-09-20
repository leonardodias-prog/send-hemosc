# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S sendhemosc && adduser -S sendhemosc -G sendhemosc
USER sendhemosc

COPY --from=build /build/target/send-hemosc-*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=postgres

ENTRYPOINT ["java", "-jar", "app.jar"]
