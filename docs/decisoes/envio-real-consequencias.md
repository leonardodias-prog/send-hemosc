# Sair do modo de teste: o que muda e o que passa a ser responsabilidade nossa

**Para:** Leonardo, Eduardo e Jean
**Data:** 21/09/2026
**Contexto:** decisão de permitir que cada pessoa cadastrada receba e-mail no próprio endereço,
em vez de tudo ser redirecionado para um endereço único de teste.

---

## O que muda na prática

Até agora, todo e-mail gerado era desviado para um endereço só. Era impossível atingir alguém
sem querer. Com a mudança, **o sistema passa a mandar mensagem de verdade para quem estiver no
banco** — e isso transforma quatro coisas em responsabilidade nossa.

## 1. Erro de código vira e-mail na caixa de estranho

Antes, um laço mal escrito mandava 120 mensagens para nós mesmos. Agora manda para 120 pessoas.
Não existe desfazer: e-mail enviado não volta.

**O que fazemos a respeito:**
- o disparo fica restrito a quem tem perfil de responsável, autenticado
- toda convocação fica registrada com quem disparou, quando e para quantos
- a rodada é interrompida após cinco falhas seguidas, para não insistir em erro de infraestrutura

**O que depende de nós:** testar alteração no envio com o modo `log` antes de ligar o real.
O modo `log` continua existindo exatamente para isso.

## 2. Dado fictício vira dado pessoal de verdade

Um cadastro com nome, e-mail, data de nascimento e **tipo sanguíneo** é dado pessoal. E tipo
sanguíneo é **dado sensível de saúde** pela LGPD, categoria com exigências mais rígidas que dado
comum.

Enquanto a base era fictícia, nada disso valia. Com gente real cadastrada, vale.

**O que isso exige:**

| Exigência | Como atendemos |
|---|---|
| Consentimento específico e destacado | Caixa própria de autorização, desmarcada por padrão |
| Finalidade informada | Termo de uso explicando para que o e-mail será usado |
| Prova do consentimento | Registro de data, hora e versão do termo aceito |
| Direito de revogar | Link de descadastro em toda mensagem |
| Direito de exclusão | Precisa existir — hoje não existe |

**Atenção:** o relatório da disciplina afirma que o projeto é protótipo com dados fictícios,
justamente por causa da LGPD. Se passarmos a cadastrar pessoas reais, **o relatório precisa ser
atualizado** — senão o documento entregue contradiz o que o sistema faz.

## 3. Quem podemos cadastrar

| Quem | Pode? | Por quê |
|---|---|---|
| Nós três | Sim | Somos os responsáveis pelo projeto |
| Colegas que concordaram em testar | Sim | Consentimento dado, finalidade clara |
| Profissionais do HEMOSC | **Não sem autorização formal** | São a comunidade parceira, não cobaias |
| Doadores reais do hemocentro | **Não** | Não temos base legal nem autorização |
| Familiares "só para testar" | Só com consentimento explícito | Vale a mesma regra |

Na dúvida: se a pessoa não sabe que está cadastrada, não cadastre.

## 4. As mensagens vão cair em spam

Estamos enviando de um endereço `@gmail.com` através da infraestrutura do Brevo. O Gmail publica
quais servidores podem enviar em nome dele, e o Brevo não está nessa lista. Para o destinatário,
o sinal é o mesmo de uma tentativa de falsificação.

**Consequência:** boa parte das mensagens cai em spam. Não tem contorno por código.

**Como resolver de vez:** registrar um domínio (`.com.br` custa cerca de R$ 40 por ano e exige
CPF) e verificá-lo no Brevo. Aí o remetente passa a ser um endereço do nosso domínio, autorizado
por registro DNS, e a entrega normaliza. É o único custo em dinheiro do projeto inteiro.

**Enquanto não houver domínio:** avisar quem for testar para conferir a caixa de spam. E não
depender disso numa apresentação ao vivo.

## 5. Limites do plano gratuito

O plano gratuito do Brevo tem limite diário de envio. Conferir o número atual na conta antes de
qualquer teste com volume. Estourar o limite bloqueia o envio até o dia seguinte — inclusive no
meio de uma apresentação.

## 6. O banco não pode mais ser em memória

Hoje o banco vive na RAM e some a cada reinício. Como o Render hiberna após quinze minutos sem
acesso, **todo usuário cadastrado desapareceria** ao acordar.

Com usuários reais, isso deixa de ser aceitável. Passamos a precisar de um banco que persista.
O PostgreSQL gratuito do Render expira em trinta dias; serviços como o Neon mantêm plano
gratuito permanente.

---

## Regras que valem a partir de agora

1. **Alteração no envio se testa com `EMAIL_MODO=log`.** Sem exceção.
2. **Ninguém é cadastrado sem saber.** Consentimento é da pessoa, não nosso.
3. **Não cadastramos profissional do HEMOSC nem doador real.** Nem "só para ver funcionando".
4. **Quem dispara convocação é usuário identificado com perfil de responsável.** A ação fica
   registrada em nome de alguém.
5. **Se a base de teste virar base real, o relatório é atualizado junto.** O documento entregue
   tem que descrever o sistema que existe.

---

## Aviso

Este documento descreve o que entendemos das obrigações envolvidas, com base no material de
referência do projeto. **Não é orientação jurídica.** A conformidade com a LGPD de um sistema
que trata dado de saúde precisa ser validada com o professor orientador e, se o projeto
avançar para uso real, com a própria instituição.
