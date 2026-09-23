-- ---------------------------------------------------------------------------
-- Liberacao manual do limite de contato.
--
-- O teto de convocacoes sem resposta se desfaz sozinho com o tempo, ou com uma
-- doacao registrada. Esta coluna permite que o administrador o desfaca na hora,
-- para uma pessoa: convocacoes enviadas antes dela deixam de contar, tanto para
-- o teto quanto para o intervalo minimo entre convocacoes.
-- ---------------------------------------------------------------------------
ALTER TABLE doador ADD COLUMN contato_liberado_em TIMESTAMP;
