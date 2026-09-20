# ADR 0001 — Arquitetura hexagonal e stack Java

**Status:** Aceito
**Data:** 18/09/2026
**Etapa do projeto:** 4 — definição técnica

---

## Contexto

A Etapa 3 do relatório fechou o escopo funcional do Send Hemosc: cadastro de doadores, controle
de estoque por tipo sanguíneo em três níveis, cálculo automático da próxima data apta, envio de
notificações por e-mail (automático e manual) e reenvio mensal a quem não compareceu. A
modelagem das entidades `doador`, `estoque` e `notificação` também ficou definida.

Restava escolher a linguagem, o framework e a organização interna do código. O documento de
referência interna sugeria "Java/JDBC ou Node, conforme a preferência do grupo", sem fechar.

Restrições que pesaram na decisão:

- prazo de uma disciplina, com três desenvolvedores;
- o sistema é um **protótipo** para demonstração, não vai a produção;
- as regras de intervalo entre doações precisam ser revisadas pela equipe de hemoterapia,
  então precisam estar legíveis e concentradas;
- o ambiente de desenvolvimento do grupo não tem Docker instalado em todas as máquinas.

## Decisão

### Linguagem e framework: Java 21 + Spring Boot 3.5

Dois dos requisitos mais trabalhosos do escopo já vêm resolvidos pelo framework, sem
biblioteca externa:

| Requisito | Resolvido por |
|---|---|
| Reenvio mensal automático | `@Scheduled(cron = ...)` |
| Envio de e-mail | `spring-boot-starter-mail` |
| Modelagem relacional | Spring Data JPA |
| Versionamento do schema | Flyway |

Pesou também que é o ecossistema em que a equipe tem mais familiaridade, reduzindo o tempo
gasto aprendendo ferramenta — tempo que, num projeto de extensão, rende mais em elicitação e
validação com a instituição do que em código.

### Organização interna: arquitetura hexagonal (Ports & Adapters)

```
domain/     regras, enums, DTOs e as interfaces (ports) que o domínio exige
usecase/    casos de uso, um por operação de negócio
adapter/
  inbound/  o que aciona a aplicação: HTTP e agendador
  outbound/ o que a aplicação aciona: persistência e e-mail
config/     parâmetros externalizados
```

Motivo principal: **o domínio não depende de infraestrutura**. `CalcularAptidaoUseCase` não
conhece banco nem JPA — recebe dados e devolve uma decisão. Isso permite testá-lo sem subir
contexto Spring e, mais importante, permite que a regra seja lida e revisada isoladamente pela
equipe de hemoterapia.

Motivo secundário: trocar a implementação de um port não toca no domínio. O envio de e-mail
tem duas implementações do mesmo port (`LogEmailAdapter` e `SmtpEmailAdapter`), escolhidas por
configuração.

### Regras de negócio externalizadas em `application.yml`

Os valores de intervalo entre doações, limite anual, faixa etária, peso mínimo e limiares de
estoque não estão no código — estão em `sendhemosc.*` no `application.yml`. A equipe do HEMOSC
pode revisar e corrigir os números sem ler Java.

### Perfil padrão sem dependências externas

O perfil `dev` usa H2 em memória e o adapter de e-mail em modo log. `mvn spring-boot:run` sobe a
aplicação completa, com massa fictícia, sem Docker, sem banco instalado e **sem enviar nenhum
e-mail**. O perfil `postgres` sobe o ambiente realista quando necessário.

## Consequências

### Positivas

- A regra de aptidão existe em **um único arquivo**, testado por 11 casos.
- Trocar H2 por PostgreSQL, ou log por SMTP, é mudança de configuração.
- O padrão de camadas rende descrição direta no relatório da disciplina.
- O disparo acidental de e-mail real exige uma mudança de configuração deliberada.

### Negativas

- Mais arquivos que uma abordagem em camadas simples (controller → service → repository).
  Para um sistema deste tamanho, é verbosidade assumida em troca de testabilidade.
- Java tem mais cerimônia que Python ou JavaScript. Lombok reduz parte disso.

### Descartadas

| Alternativa | Por que não |
|---|---|
| Next.js + Prisma | Bom deploy e uma linguagem só, mas a equipe é mais forte em Java |
| Django | O admin pronto economizaria telas, mas afasta do conteúdo da disciplina |
| MongoDB | O domínio é essencialmente relacional (doador → doações → notificações) |

## Referências

- Etapa 3 do relatório: definição de escopo e modelagem das entidades
- Documento de referência interna, seção 5 (catálogo de ideias I2 e I3) e seção 10 (LGPD)
