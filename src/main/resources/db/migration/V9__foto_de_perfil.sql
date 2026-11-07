-- ---------------------------------------------------------------------------
-- Foto de perfil.
--
-- Fica em tabela propria, e nao em coluna de usuario: a conta e lida em quase toda requisicao
-- (entrada, menus, auditoria) e nenhuma dessas leituras precisa dos bytes da imagem. A foto vai
-- para o banco, e nao para disco, porque o disco da hospedagem some a cada publicacao. Ela ja
-- chega reduzida (ate 256 x 256, JPEG), entao cada linha pesa poucas dezenas de KB.
-- ---------------------------------------------------------------------------
CREATE TABLE usuario_foto (
    usuario_id     BIGINT      PRIMARY KEY,
    tipo_conteudo  VARCHAR(30) NOT NULL,
    conteudo       BYTEA       NOT NULL,
    atualizado_em  TIMESTAMP   NOT NULL,
    CONSTRAINT fk_usuario_foto_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);
