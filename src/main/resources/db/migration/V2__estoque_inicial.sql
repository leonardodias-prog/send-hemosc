-- Cria a linha de estoque para os oito tipos sanguineos.
-- Capacidade alvo ficticia, apenas para demonstrar a classificacao de niveis.
INSERT INTO estoque_hemocomponente (tipo_sanguineo, quantidade_bolsas, capacidade_alvo) VALUES
    ('A+',  72, 120),
    ('A-',  14,  50),
    ('B+',  40,  70),
    ('B-',   9,  30),
    ('AB+', 22,  35),
    ('AB-',  6,  20),
    ('O+',  95, 180),
    ('O-',  11,  60);
