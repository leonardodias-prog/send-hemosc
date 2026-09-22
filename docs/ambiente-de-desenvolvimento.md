# Rodando o projeto na sua máquina

**Para:** Eduardo e Jean
**Resumo:** você não precisa de conta em serviço nenhum. Clone, rode, funciona.

---

## O básico

**Pré-requisitos:** Java 21 e Maven. Nada além disso.

```bash
git clone https://github.com/leonardodias-prog/send-hemosc.git
cd send-hemosc
mvn spring-boot:run
```

Abra `http://localhost:8080`.

Na primeira subida o sistema mostra no console algo assim:

```
===========================================================================
 ADMINISTRADOR CRIADO

 E-mail : admin@sendhemosc.local
 Senha  : <gerada aleatoriamente>
===========================================================================
```

Use esses dados para entrar. Se preferir escolher a senha:

```bash
MASTER_EMAIL=voce@example.org MASTER_SENHA=suaSenhaAqui mvn spring-boot:run
```

## O que você ganha sem configurar nada

| | |
|---|---|
| Banco | H2 em memória, criado pelas migrations a cada subida |
| Dados | 120 doadores fictícios, estoque dos 8 tipos sanguíneos |
| E-mail | Modo log: as mensagens aparecem no console, nada sai |
| Console do banco | `http://localhost:8080/h2-console` |

O estado some quando você para a aplicação. É proposital: cada execução começa do zero, sem depender de nada que alguém tenha deixado configurado.

### Console do banco

Em `http://localhost:8080/h2-console`, preencha:

```
JDBC URL : jdbc:h2:mem:sendhemosc
User     : sa
Password : (vazio)
```

Dá para rodar SQL direto nas tabelas, como em qualquer Postgres.

## Por que não usamos o banco de produção para desenvolver

Três motivos, e o primeiro basta:

1. **Ele tem dados de pessoas reais.** A base publicada guarda nome, e-mail e tipo sanguíneo de quem se cadastrou. Tipo sanguíneo é dado sensível de saúde pela LGPD. Um `DELETE` sem `WHERE` durante um teste não tem desfazer.
2. **Ninguém fica esperando ninguém.** Com banco local, seu teste não depende de outra pessoa não estar mexendo.
3. **Não precisa de credencial.** Segredo que não existe não vaza.

## Se precisar mesmo ver os dados publicados

Fale com o Leonardo. Ele cria um acesso de leitura no Neon, separado da conta dele, e você conecta por DBeaver ou pgAdmin. Esse acesso só lê: não altera dados nem mexe na configuração do serviço.

## Se quiser um banco na nuvem, seu

Crie sua própria conta gratuita no [Neon](https://neon.tech), faça um projeto e rode com:

```bash
SPRING_PROFILES_ACTIVE=producao \
DB_URL='jdbc:postgresql://SEU_HOST/SEU_BANCO?sslmode=require' \
DB_USER=seu_usuario \
DB_PASSWORD=sua_senha \
mvn spring-boot:run
```

O banco é seu, isolado, e nada que você fizer afeta o dos outros.

## Antes de abrir pull request

```bash
mvn clean verify
```

Isso roda os testes e o Checkstyle. A integração contínua roda o mesmo a cada push, mais as migrations contra um PostgreSQL de verdade — vale rodar antes para não descobrir depois.

## Perfis disponíveis

| Perfil | Quando usar |
|---|---|
| `dev` | Padrão. H2 em memória, dados fictícios, e-mail em log |
| `producao` | Exige PostgreSQL. É o que roda no ambiente publicado |

## Cuidados

> [!CAUTION]
> Antes de mexer em qualquer coisa ligada a envio de e-mail, leia
> [`docs/decisoes/envio-real-consequencias.md`](decisoes/envio-real-consequencias.md).
> O sistema hoje envia de verdade para as pessoas cadastradas, e mensagem enviada não volta.

> [!WARNING]
> Nunca faça commit de arquivo `.env`, string de conexão, chave de API ou senha. O repositório
> é público e o histórico do git guarda para sempre, mesmo depois de apagar.
