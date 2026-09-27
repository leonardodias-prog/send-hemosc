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

# Ajustado para o container de 512 MB e uma CPU do plano gratuito:
#  - MaxRAMPercentage=60 deixa folga para metaspace, code cache e threads, que somados
#    passam de 100 MB em uma aplicacao Spring Boot
#  - TieredStopAtLevel=1 limita a compilacao JIT ao primeiro nivel. Isso reduz o desempenho
#    de regime, que aqui nao importa, em troca de subida mais rapida. O servico hiberna por
#    inatividade, entao quem abre o link paga o tempo de subida, e nao o de regime.
# O treino do CDS abaixo usa as mesmas opcoes: arquivo gerado com outro coletor de lixo e
# ignorado na subida.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

# Jar extraido: abrir classes de dentro de jar aninhado e mais lento, e o CDS so aceita
# classpath de arquivos comuns.
COPY --from=build /build/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extracted && rm app.jar

# Classpath explicito em vez de -jar. Com Java 21, o CDS descartou as classes das dependencias
# listadas no Class-Path do manifest ("Unsupported location") e a subida nao ganhou nada; com os
# jars no -cp, o arquivo sai completo. O app.args reproduz o manifest: mesma ordem de jars e
# mesma classe principal. O treino e a subida leem o mesmo arquivo, porque o CDS so vale quando
# o classpath das duas e identico.
#
# O manifest quebra linhas longas em 72 colunas, continuando com um espaco no inicio; o primeiro
# awk junta essas linhas antes de ler os campos.
RUN unzip -p extracted/app.jar META-INF/MANIFEST.MF | tr -d '\r' \
    | awk '/^ /{ linha = linha substr($0, 2); next }; { if (linha != "") print linha; linha = $0 }; END { print linha }' \
    | awk -F': ' '$1 == "Main-Class" { main = $2 }; $1 == "Class-Path" { n = split($2, jars, " "); for (i = 1; i <= n; i++) cp = cp ":extracted/" jars[i] }; END { print "-cp extracted/app.jar" cp " " main }' \
    > app.args

# Class Data Sharing: uma subida de treino grava as classes carregadas ja analisadas em
# app.jsa, e a subida real le dali em vez de repetir o trabalho. Medido com Java 21, a subida
# caiu de ~12 s para ~5 s. Precisa ser gerado aqui, na mesma JVM que roda a aplicacao.
#
# O treino sobe com o perfil dev (H2 em memoria, sem massa ficticia) porque no build nao ha
# banco, e para assim que o contexto termina de subir, antes de qualquer runner. As classes do
# PostgreSQL ficam de fora do arquivo e carregam do jeito normal. Se o arquivo nao servir, a
# JVM apenas avisa e sobe sem ele.
RUN java $JAVA_OPTS -XX:ArchiveClassesAtExit=app.jsa \
        -Dspring.profiles.active=dev \
        -Dsendhemosc.seed.habilitado=false \
        -Dspring.context.exit=onRefresh \
        -Dlogging.level.root=WARN \
        -Dlogging.level.br.univille.sendhemosc=WARN \
        -Dlogging.level.org.hibernate.SQL=WARN \
        @app.args

USER app

# Perfil publicado: PostgreSQL, sem massa ficticia, e-mail em log por padrao.
ENV SPRING_PROFILES_ACTIVE=producao

# A plataforma injeta PORT; 8080 e o padrao local.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -XX:SharedArchiveFile=app.jsa @app.args"]
