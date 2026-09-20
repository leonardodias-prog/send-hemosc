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

# Ajustado para o container de 512 MB do plano gratuito: 60% de heap deixa folga para
# metaspace, code cache e threads, que somados passam de 100 MB em uma app Spring Boot.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -Xss512k -jar app.jar"]
