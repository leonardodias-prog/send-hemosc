# ---- Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Baixar as dependencias antes de copiar o codigo aproveita o cache de camada do Docker:
# enquanto o pom nao muda, esta etapa nao se repete. E apenas uma otimizacao, e o goal
# dependency:go-offline falha com alguma facilidade em rede instavel, entao um erro aqui nao
# interrompe o build: o package seguinte baixa o que faltar.
COPY pom.xml .
RUN mvn -B dependency:go-offline || echo "go-offline incompleto, o package baixa o restante"

# Sem -q: se o build quebrar, a mensagem precisa aparecer no log da plataforma.
# Sem clean: o container e novo, nao ha nada para limpar.
COPY src ./src
RUN mvn -B package -DskipTests

# Nome fixo, para a etapa seguinte nao depender de curinga.
RUN cp target/send-hemosc-*.jar /build/app.jar

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/app.jar app.jar

# Perfil publicado: PostgreSQL, sem massa ficticia, e-mail em log por padrao.
ENV SPRING_PROFILES_ACTIVE=producao

# A plataforma injeta PORT; 8080 e o padrao local.
ENV PORT=8080
EXPOSE 8080

# Ajustado para o container de 512 MB e uma CPU do plano gratuito:
#  - MaxRAMPercentage=60 deixa folga para metaspace, code cache e threads, que somados
#    passam de 100 MB em uma aplicacao Spring Boot
#  - TieredStopAtLevel=1 limita a compilacao JIT ao primeiro nivel. Isso reduz o desempenho
#    de regime, que aqui nao importa, em troca de subida mais rapida. O servico hiberna por
#    inatividade, entao quem abre o link paga o tempo de subida, e nao o de regime.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -jar app.jar"]
