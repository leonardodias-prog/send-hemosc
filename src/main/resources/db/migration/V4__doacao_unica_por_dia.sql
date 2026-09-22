-- ---------------------------------------------------------------------------
-- Uma pessoa nao doa duas vezes no mesmo dia.
--
-- A restricao fica no banco, e nao apenas na aplicacao, porque o calculo de
-- aptidao le a ultima doacao e conta as doacoes dos ultimos doze meses. Um
-- registro duplicado, vindo de um clique repetido ou de uma importacao futura,
-- distorceria o intervalo e o limite anual de quem doou.
-- ---------------------------------------------------------------------------
ALTER TABLE doacao ADD CONSTRAINT uk_doacao_doador_data UNIQUE (doador_id, data_doacao);

-- Sustenta a consulta que fecha as convocacoes pendentes quando uma doacao e registrada.
CREATE INDEX idx_notificacao_comparecimento
    ON notificacao (doador_id, status, compareceu_em);
