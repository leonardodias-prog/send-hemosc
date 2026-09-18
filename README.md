# Send Hemosc

Protótipo de sistema que relaciona o **estoque de hemocomponentes por tipo sanguíneo** ao
**cadastro individual de doadores**, para identificar quem está apto a doar e direcionar a
comunicação a esse público específico.

Projeto de Extensão — **Vivências de Extensão III** · Curricularização da Extensão
Universidade da Região de Joinville (UNIVILLE) · 2026

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

| Funcionalidade | Descrição |
|---|---|
| Cadastro de doadores | Dados de contato, tipo sanguíneo e histórico de doações |
| Controle de estoque | Por tipo sanguíneo, com níveis **normal**, **atenção** e **crítico** |
| Cálculo de aptidão | Data estimada da próxima doação, a partir da última doação registrada |
| Notificação por e-mail | Envio automático e manual, segmentado por tipo sanguíneo + aptidão |
| Reenvio mensal | Para quem ainda não compareceu, enquanto o tipo permanecer em nível baixo |

**Objetivo geral:** demonstrar, por meio de um protótipo, que o cruzamento entre a situação do
estoque e a aptidão individual dos doadores permite uma captação mais direcionada e menos
dependente de campanhas genéricas.

## Escopo e limites

> [!IMPORTANT]
> Este é um **protótipo em ambiente de testes, alimentado por dados fictícios**.
> Não há previsão de implantação em produção.

A decisão de manter o projeto como protótipo é deliberada e considera que:

- o sistema trataria **dados pessoais e de saúde**, sujeitos à LGPD;
- as **regras de intervalo entre doações** precisam ser validadas pela equipe de hemoterapia
  antes de qualquer uso real.

A finalidade é apresentar o resultado aos profissionais da instituição para avaliação e
devolutiva quanto a uma possível melhoria futura do processo de captação.

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

Cronograma organizado em etapas sequenciais, cada uma produzindo um resultado verificável
antes do início da seguinte.

| # | Etapa | Planejado | Realizado | Status |
|---|---|---|---|---|
| 1 | Definição do tema e formação do grupo | 12/08/2026 | 12/08/2026 | Concluída |
| 2 | Levantamento do problema junto à instituição | 12/08/2026 | 18/08/2026 | Concluída |
| 3 | Elaboração da proposta técnica e definição do escopo | 19/08/2026 | 26/08/2026 | Concluída |
| 4 | — | — | — | Pendente |
| 5 | — | — | — | Pendente |
| 6 | — | — | — | Pendente |
| 7 | — | — | — | Pendente |

**Stack técnica:** ainda não definida. O documento de referência sugere back-end com banco
relacional (`doador` → `doacoes` → `notificacoes`) e front-end web, em Java/JDBC ou Node,
conforme a preferência do grupo.

## Estrutura do repositório

```
send-hemosc/
├── docs/
│   ├── base/       # Documentos originais do projeto (relatório e referência interna)
│   └── ata/        # Atas de reunião e registros de contato com a instituição
├── src/            # Código-fonte do protótipo
└── README.md
```

## Documentos

- [`docs/base/Relatorio_VDEX_III_Send_Hemosc.docx`](docs/base/Relatorio_VDEX_III_Send_Hemosc.docx) —
  relatório da disciplina (entrega parcial)
- [`docs/base/Projeto_Extensao_Hemosc_Joinville.pdf`](docs/base/Projeto_Extensao_Hemosc_Joinville.pdf) —
  documento de referência interna: roteiro em 9 fases, catálogo de ideias, roteiro de
  entrevista e orientações de LGPD
