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
| Convocação por tipo sanguíneo + aptidão | [`ConvocarDoadoresUseCase`](src/main/java/br/univille/sendhemosc/usecase/notificacao/ConvocarDoadoresUseCase.java) |
| Convocação de uma seleção específica | [`ConvocarSelecionadosUseCase`](src/main/java/br/univille/sendhemosc/usecase/notificacao/ConvocarSelecionadosUseCase.java) |
| Limite de contato por pessoa: intervalo e teto de convocações sem resposta | [`AvaliarLimiteDeContatoUseCase`](src/main/java/br/univille/sendhemosc/usecase/notificacao/AvaliarLimiteDeContatoUseCase.java) |
| Disparo automático configurável na tela | [`ExecutarDisparoAutomaticoUseCase`](src/main/java/br/univille/sendhemosc/usecase/notificacao/ExecutarDisparoAutomaticoUseCase.java) |
| Interruptor de envio, acionável na tela | [`EnvioDeEmailRouter`](src/main/java/br/univille/sendhemosc/adapter/outbound/email/EnvioDeEmailRouter.java) |
| Descadastro exigido pela LGPD | [`DescadastroViewAdapter`](src/main/java/br/univille/sendhemosc/adapter/inbound/http/controller/DescadastroViewAdapter.java) |

A convocação por tipo não busca apenas o tipo exato em falta: usa a **tabela de compatibilidade
transfusional**. Quando A+ está em falta, convoca A+, A−, O+ e O−.

## Telas

| Rota | Quem acessa | Para quê |
|---|---|---|
| `/` | Todos autenticados | Painel de estoque, cadastro de doador, interruptor de envio |
| `/doadores` | Todos autenticados | Busca com filtros, aptidão e convocação seletiva |
| `/disparo-automatico` | Responsável e administrador | Liga, desliga e configura a rodada automática, com prévia de quem ela convocaria |
| `/usuarios` | Administrador | Criação, alteração, senha e exclusão de contas |
| `/login`, `/cadastro`, `/termo` | Público | Entrada, autocadastro e termo de uso |

## Perfis de acesso

A separação segue o efeito de cada ação. Mandar e-mail sai do sistema e não se desfaz;
alimentar dados, não.

| Perfil | Pode | Como entra |
|---|---|---|
| **Operador** | Cadastra doadores, atualiza estoque, consulta | Autocadastro, ativo na hora |
| **Responsável** | Tudo acima, mais disparar convocações, ligar o envio e o disparo automático | Autocadastro, pendente até aprovação |
| **Administrador** | Tudo acima, mais gerenciar contas | Criado na primeira subida |

O cadastro de responsável dispara um e-mail ao administrador com links de aprovar e recusar.
O token vale uma vez só.

## Rodando o projeto

**Pré-requisitos:** Java 21 e Maven. Nada além disso — sem banco instalado, sem conta em
serviço nenhum.

```bash
mvn spring-boot:run
```

Abra `http://localhost:8080`. Sobe com H2 em memória, migrations aplicadas, 120 doadores
fictícios e os oito tipos com estoque. **Nenhum e-mail sai**: as mensagens vão para o log.

A senha do administrador aparece no console da primeira subida. Guia completo para quem entra
no projeto: [`docs/ambiente-de-desenvolvimento.md`](docs/ambiente-de-desenvolvimento.md).

### Testes

```bash
mvn clean verify
```

117 testes. O Checkstyle roda na fase `validate` e quebra o build em violação. A integração
contínua roda o mesmo, mais as migrations contra um PostgreSQL de verdade.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.6 |
| Segurança | Spring Security · BCrypt |
| Persistência | Spring Data JPA + Hibernate |
| Banco | H2 em memória (`dev`) · PostgreSQL no Neon (`producao`) |
| Migrations | Flyway |
| E-mail | Brevo, pela API HTTP |
| Telas | Thymeleaf |
| Agendador | `@Scheduled` |
| Testes | JUnit 5 · Mockito · AssertJ |
| Qualidade | Checkstyle · JaCoCo |
| Massa de dados | Datafaker |
| Build | Maven · Docker |
| CI | GitHub Actions |

## Arquitetura

Hexagonal (Ports & Adapters). Decisão registrada em
[`docs/adr/0001`](docs/adr/0001-arquitetura-hexagonal-e-stack-java.md).

```
br/univille/sendhemosc/
├── domain/          regras, enums, DTOs e as interfaces (ports)
│   ├── enums/       TipoSanguineo, NivelEstoque, PerfilUsuario, SituacaoUsuario
│   ├── exception/   catálogos de erro por contexto
│   └── port/        contratos que o domínio exige da infraestrutura
├── usecase/         um caso de uso por operação de negócio
│   ├── doador/
│   ├── estoque/
│   ├── notificacao/
│   └── usuario/
├── adapter/
│   ├── inbound/     HTTP e agendador
│   └── outbound/    persistência JPA e envio de e-mail
└── config/          segurança e parâmetros externalizados
```

O domínio não conhece banco nem provedor de e-mail. `CalcularAptidaoUseCase` recebe dados e
devolve uma decisão — sem Spring, sem JPA. Foi o que permitiu trocar o provedor de e-mail duas
vezes sem tocar em uma linha de regra de negócio.

## Regras de negócio

> [!CAUTION]
> As regras de aptidão em `application.yml` reproduzem a normativa de hemoterapia, mas
> **precisam ser validadas formalmente pela equipe do HEMOSC** antes de qualquer uso que não
> seja demonstração. Informação errada aqui gera dano real.

Ficam em `sendhemosc.aptidao`, fora do código, para serem revisadas sem ler Java:

| Regra | Masculino | Feminino |
|---|---|---|
| Intervalo mínimo entre doações | 60 dias | 90 dias |
| Máximo em 12 meses | 4 | 3 |
| Faixa etária | 16 a 69 anos | 16 a 69 anos |
| Peso mínimo | 50 kg | 50 kg |

Limiares de estoque: abaixo de **30%** da capacidade alvo é `CRITICO`, abaixo de **60%** é
`ATENCAO`, o resto é `NORMAL`.

**Limite de contato por pessoa**, em `sendhemosc.notificacao`: cada pessoa recebe no máximo
**3** convocações sem resposta, com pelo menos **30 dias** entre uma e outra. Vale para toda
convocação, manual ou automática — para quem recebe, o e-mail é o mesmo, seja qual for o botão
que o disparou.

O teto se desfaz de três formas: cada convocação sem resposta deixa de contar depois de
**180 dias**; uma doação registrada fecha as pendentes e zera a contagem; e o administrador pode
liberar uma pessoa na hora, pela tela de doadores, com registro em auditoria.

## Envio de e-mail

Duas condições precisam valer para uma mensagem sair:

1. `EMAIL_MODO=brevo` com `BREVO_API_KEY` definida — configuração, muda com reinício
2. Interruptor ligado no painel — decisão imediata do responsável, sem reinício

Com qualquer uma delas ausente, as mensagens apenas vão para o log. Toda mudança do
interruptor e toda convocação ficam em auditoria, com quem fez e quando.

`EMAIL_DESTINATARIO_TESTE` desvia todas as mensagens para endereços conhecidos, seja qual for
o doador. Serve para conferir um envio sem atingir ninguém.

O **disparo automático** nasce desligado e é configurado em `/disparo-automatico`: tipos
sanguíneos considerados, limite de envios por rodada, janela de horário e frequência. A rotina
confere a cada dez minutos se a rodada é devida, e não em hora fixa: na hospedagem gratuita o
serviço hiberna sem acesso, e a conferência aproveita o momento em que ele acorda. A rodada usa o
mesmo envio do painel — com o interruptor desligado, as convocações só vão para o log.

> [!WARNING]
> **SMTP não funciona em hospedagem gratuita.** A porta de saída é bloqueada para conter spam,
> e o envio falha com `SocketTimeoutException`. Por isso a entrega é por API HTTP.

## LGPD

O sistema trata **tipo sanguíneo**, que é dado pessoal sensível de saúde. O que está
implementado:

- consentimento como caixa própria, não marcada por padrão;
- histórico do consentimento com data, hora e versão do termo aceito;
- link de descadastro em toda mensagem, de uso imediato;
- página de termo de uso com finalidade e forma de revogação;
- auditoria de quem disparou cada convocação.

Antes de cadastrar pessoas reais, leia
[`docs/decisoes/envio-real-consequencias.md`](docs/decisoes/envio-real-consequencias.md).

## Publicação

Roda no [Render](https://render.com), com PostgreSQL no [Neon](https://neon.tech) e e-mail pelo
[Brevo](https://brevo.com) — os três em plano gratuito. O blueprint está em
[`render.yaml`](render.yaml); as variáveis de ambiente, documentadas em
[`.env.example`](.env.example).

Cada `git push` na `main` republica sozinho.

> [!NOTE]
> No plano gratuito o serviço hiberna após ~15 minutos sem acesso, e a primeira subida depois
> disso leva cerca de dois minutos e meio. Se for apresentar ao vivo, abra o link antes.

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
| Leonardo Dias | Desenvolvimento e infraestrutura |
| Eduardo da Costa | Desenvolvimento |
| Jean Nack | Desenvolvimento |

**Orientação:** Prof. Sergio Odilon Fischer

## Documentos

- [`docs/ambiente-de-desenvolvimento.md`](docs/ambiente-de-desenvolvimento.md) — como rodar
- [`docs/decisoes/`](docs/decisoes) — consequências das decisões tomadas
- [`docs/adr/`](docs/adr) — decisões de arquitetura
- [`docs/base/`](docs/base) — relatório da disciplina e documento de referência interna
- [`docs/ata/`](docs/ata) — atas de reunião e notas de entrevista
