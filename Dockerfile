# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline -q

COPY src ./src
RUN mvn -B clean package -DskipTests -q

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/send-hemosc-*.jar app.jar

# Perfil de demonstracao: H2 em memoria, dados ficticios, e-mail apenas em log.
ENV SPRING_PROFILES_ACTIVE=demo

# A plataforma injeta PORT; 8080 e o padrao local.
ENV PORT=8080
EXPOSE 8080

# MaxRAMPercentage mantem a JVM dentro do limite de container pequeno (512 MB no plano free).
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -jar app.jar"]
