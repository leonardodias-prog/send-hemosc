# Send Hemosc

Protótipo de sistema que relaciona o **estoque de hemocomponentes por tipo sanguíneo** ao
**cadastro individual de doadores**, para identificar quem está apto a doar e direcionar a
comunicação a esse público específico.

Projeto de Extensão — **Vivências de Extensão III** · Curricularização da Extensão
Universidade da Região de Joinville (UNIVILLE) · 2026

[![CI](https://github.com/leonardodias-prog/send-hemosc/actions/workflows/ci.yml/badge.svg)](https://github.com/leonardodias-prog/send-hemosc/actions/workflows/ci.yml)

---

## O problema

Hoje, estoque e cadastro de doadores são acompanhados de forma independente. Como os dois
dados não se comunicam, a captação é feita por **campanhas genéricas**, dirigidas a todo o
público cadastrado, sem considerar:

- se a pessoa tem o tipo sanguíneo necessário naquele momento;
- se ela já cumpriu o intervalo mínimo entre doações.

O resultado é uma comunicação pouco direcionada e uma atuação **reativa** — que começa só
depois que o estoque já caiu.

## A proposta

| Funcionalidade | Onde está no código |
|---|---|
| Classificação do estoque em **normal / atenção / crítico** | [`ClassificarNivelEstoqueUseCase`](src/main/java/br/univille/sendhemosc/usecase/estoque/ClassificarNivelEstoqueUseCase.java) |
| Cálculo da data estimada da próxima doação | [`CalcularAptidaoUseCase`](src/main/java/br/univille/sendhemosc/usecase/doador/CalcularAptidaoUseCase.java) |
| Convocação segmentada por tipo + aptidão | [`ConvocarDoadoresUseCase`](src/main/java/br/univille/sendhemosc/usecase/notificacao/ConvocarDoadoresUseCase.java) |
| Disparo automático diário | [`ConvocacaoScheduler`](src/main/java/br/univille/sendhemosc/adapter/inbound/scheduler/ConvocacaoScheduler.java) |
| Descadastro exigido pela LGPD | [`DescadastroApiAdapter`](src/main/java/br/univille/sendhemosc/adapter/inbound/http/controller/DescadastroApiAdapter.java) |

A convocação não busca apenas o tipo exato em falta: usa a **tabela de compatibilidade
transfusional**. Quando A+ está em falta, convoca A+, A−, O+ e O−.

## Rodando o projeto

**Pré-requisitos:** Java 21 e Maven. Nada além disso.

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com H2 em memória, 120 doadores fictícios e os oito
tipos sanguíneos já com estoque. **Nenhum e-mail é enviado** — o adapter padrão apenas registra
a mensagem no log.

### Experimentando

```bash
curl http://localhost:8080/api/estoque
```

```bash
curl -X POST "http://localhost:8080/api/convocacoes/O-"
```

Resposta:

```json
{"tipoSanguineo":"O-","nivel":"CRITICO","totalElegiveis":12,"totalEnviados":12,"totalFalhas":0}
```

Convocar um tipo em nível `NORMAL` retorna zero envios — é o comportamento esperado, o sistema
só dispara para `ATENCAO` e `CRITICO`. Para forçar, use `?ignorarNivel=true`.

| Endpoint | Método | Para quê |
|---|---|---|
| `/api/estoque` | GET | Situação dos oito tipos, ordenada por criticidade |
| `/api/estoque/{sigla}?quantidadeBolsas=N` | PUT | Atualiza o estoque |
| `/api/convocacoes/{sigla}` | POST | Dispara convocação manual |
| `/descadastro/{token}` | GET | Link de cancelamento do e-mail |
| `/h2-console` | GET | Banco em memória (perfil `dev`) |
| `/actuator/health` | GET | Health check |

### Com PostgreSQL e caixa de e-mail

Requer Docker.

```bash
docker compose up -d
```

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

MailHog captura todo e-mail enviado em `http://localhost:8025`, sem nada sair para a internet.

### Testes

```bash
mvn clean verify
```

42 testes. O Checkstyle roda na fase `validate` e quebra o build em violação.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Persistência | Spring Data JPA + Hibernate |
| Banco | H2 (dev) · PostgreSQL 16 (perfil `postgres`) |
| Migrations | Flyway |
| E-mail | Spring Mail · MailHog/Mailtrap em desenvolvimento |
| Agendador | `@Scheduled` |
| Template de e-mail | Thymeleaf |
| Testes | JUnit 5 · Mockito · AssertJ |
| Qualidade | Checkstyle · JaCoCo |
| Massa de dados | Datafaker |
| Build | Maven |
| CI | GitHub Actions |

## Arquitetura

Hexagonal (Ports & Adapters). Decisão registrada em
[`docs/adr/0001`](docs/adr/0001-arquitetura-hexagonal-e-stack-java.md).

```
br/univille/sendhemosc/
├── domain/          regras, enums, DTOs e as interfaces (ports)
│   ├── enums/       TipoSanguineo, NivelEstoque, Sexo, StatusNotificacao
│   ├── exception/   catálogos de erro por contexto
│   └── port/        contratos que o domínio exige da infraestrutura
├── usecase/         um caso de uso por operação de negócio
│   ├── doador/
│   ├── estoque/
│   └── notificacao/
├── adapter/
│   ├── inbound/     HTTP e agendador
│   └── outbound/    persistência JPA e envio de e-mail
└── config/          parâmetros externalizados
```

O domínio não conhece banco nem framework de e-mail. `CalcularAptidaoUseCase` recebe dados e
devolve uma decisão — sem Spring, sem JPA.

## Regras de negócio

> [!CAUTION]
> As regras de aptidão em `application.yml` reproduzem a normativa de hemoterapia, mas
> **precisam ser validadas formalmente pela equipe do HEMOSC** antes de qualquer uso que não
> seja demonstração. Informação errada aqui gera dano real.

Ficam todas em `sendhemosc.aptidao`, fora do código, para que possam ser revisadas sem ler Java:

| Regra | Masculino | Feminino |
|---|---|---|
| Intervalo mínimo entre doações | 60 dias | 90 dias |
| Máximo em 12 meses | 4 | 3 |
| Faixa etária | 16 a 69 anos | 16 a 69 anos |
| Peso mínimo | 50 kg | 50 kg |

Limiares de estoque: abaixo de **30%** da capacidade alvo é `CRITICO`, abaixo de **60%** é
`ATENCAO`, o resto é `NORMAL`.

## Escopo e limites

> [!IMPORTANT]
> Este é um **protótipo em ambiente de testes, alimentado por dados fictícios**.
> Não há previsão de implantação em produção.

A decisão de manter o projeto como protótipo é deliberada e considera que:

- o sistema trataria **dados pessoais e de saúde**, sujeitos à LGPD;
- as **regras de intervalo entre doações** precisam ser validadas pela equipe de hemoterapia
  antes de qualquer uso real.

### Proteções contra envio acidental

Um protótipo que dispara e-mail é um risco real. Três barreiras:

1. O adapter padrão (`LogEmailAdapter`) **não envia nada** — só registra no log. Enviar de
   verdade exige mudar `sendhemosc.email.modo` para `smtp` deliberadamente.
2. A massa fictícia usa apenas o domínio reservado `@example.org`, que não entrega a ninguém.
3. O perfil `postgres` aponta para MailHog, uma caixa falsa local.

> [!WARNING]
> Nenhum dado real de doador, paciente ou estoque deve ser commitado neste repositório.

## Instituição parceira

**Hospital Regional Hans Dieter Schmidt** — Joinville/SC, unidade que abriga a Agência
Transfusional do HEMOSC. O contato foi estabelecido por intermédio de uma enfermeira da
unidade, e participam do projeto três profissionais da instituição como comunidade parceira.

O HEMOSC responde por cerca de 98% da coleta e distribuição de sangue em Santa Catarina. A
unidade de Joinville é hemocentro regional e cobre ainda municípios do Norte catarinense —
Jaraguá do Sul, Guaramirim, São Francisco do Sul, Araquari, Itapoá e Massaranduba.

## Equipe

| Integrante | Papel |
|---|---|
| Leonardo Dias | Desenvolvimento |
| Eduardo da Costa | Desenvolvimento |
| Jean Nack | Desenvolvimento |

**Orientação:** Prof. Sergio Odilon Fischer

## Status

| # | Etapa | Planejado | Realizado | Status |
|---|---|---|---|---|
| 1 | Definição do tema e formação do grupo | 12/08/2026 | 12/08/2026 | Concluída |
| 2 | Levantamento do problema junto à instituição | 12/08/2026 | 18/08/2026 | Concluída |
| 3 | Elaboração da proposta técnica e definição do escopo | 19/08/2026 | 26/08/2026 | Concluída |
| 4 | Definição da arquitetura e do ambiente de desenvolvimento | — | 18/09/2026 | Concluída |
| 5 | — | — | — | Pendente |
| 6 | — | — | — | Pendente |
| 7 | — | — | — | Pendente |

## Documentos

- [`docs/base/`](docs/base) — relatório da disciplina e documento de referência interna
- [`docs/adr/`](docs/adr) — decisões de arquitetura registradas
- [`docs/ata/`](docs/ata) — atas de reunião e notas de entrevista
