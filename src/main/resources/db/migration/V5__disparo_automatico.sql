-- ---------------------------------------------------------------------------
-- Configuracao do disparo automatico de convocacoes.
--
-- O agendador existia com uma expressao cron fixa em variavel de ambiente, e
-- desligado. A partir daqui quem decide se ele roda, para quais tipos, quantos
-- por rodada e em que horario e o responsavel, pela tela, e a decisao fica
-- registrada em nome dele.
--
-- Uma linha so: e configuracao do sistema, nao cadastro.
-- ---------------------------------------------------------------------------
CREATE TABLE disparo_automatico (
    id                    BIGINT        PRIMARY KEY,
    ativo                 BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Siglas separadas por virgula, na ordem do enum: A+,A-,B+,...
    tipos_sanguineos      VARCHAR(40)   NOT NULL,
    limite_por_rodada     INTEGER       NOT NULL,
    -- Janela em horas cheias, no fuso de Sao Paulo: das hora_inicio as hora_fim, sem incluir o fim
    hora_inicio           INTEGER       NOT NULL,
    hora_fim              INTEGER       NOT NULL,
    intervalo_dias        INTEGER       NOT NULL,
    ultima_rodada_em      TIMESTAMP,
    ultima_rodada_resumo  VARCHAR(300),
    atualizado_por        VARCHAR(180),
    atualizado_em         TIMESTAMP,
    CONSTRAINT ck_disparo_linha_unica CHECK (id = 1),
    CONSTRAINT ck_disparo_limite CHECK (limite_por_rodada > 0),
    CONSTRAINT ck_disparo_janela CHECK (hora_inicio >= 0 AND hora_fim <= 24 AND hora_inicio < hora_fim),
    CONSTRAINT ck_disparo_intervalo CHECK (intervalo_dias > 0)
);

-- Nasce desligado. Ligar o envio automatico de e-mail para pessoas de verdade e decisao de
-- alguem identificado, e nao comportamento padrao do sistema.
INSERT INTO disparo_automatico (id, ativo, tipos_sanguineos, limite_por_rodada, hora_inicio, hora_fim, intervalo_dias)
VALUES (1, FALSE, 'A+,A-,B+,B-,AB+,AB-,O+,O-', 50, 8, 18, 1);
