package br.univille.sendhemosc.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.univille.sendhemosc.adapter.outbound.persistence.entity.AuditoriaEntity;
import br.univille.sendhemosc.adapter.outbound.persistence.repository.AuditoriaJpaRepository;
import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.enums.SituacaoUsuario;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Auditoria grava quem fez a acao")
class AuditoriaComAutorTest {

    private static final String EMAIL = "autor.auditoria@example.org";

    @Autowired
    private IAuditoriaPort auditoria;

    @Autowired
    private AuditoriaJpaRepository auditoriaRepository;

    @Autowired
    private IUsuarioRepositoryPort usuarioRepository;

    private AuditoriaEntity registroDe(final String acao) {
        return auditoriaRepository.findAll().stream()
                .filter(registro -> acao.equals(registro.getAcao()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @WithMockUser(username = EMAIL, roles = "RESPONSAVEL")
    @DisplayName("usuario autenticado fica com identificador e e-mail")
    void gravaIdentificadorDoAutor() {
        final Long id = usuarioRepository.criar("Autora", EMAIL, "hash", PerfilUsuario.RESPONSAVEL,
                SituacaoUsuario.ATIVO, null);

        auditoria.registrar("TESTE_AUTOR", "detalhe");

        final AuditoriaEntity registro = registroDe("TESTE_AUTOR");
        assertThat(registro.getUsuarioId()).isEqualTo(id);
        assertThat(registro.getUsuarioEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("acao sem ninguem autenticado fica como sistema, sem identificador")
    void acaoDoSistemaNaoTemIdentificador() {
        auditoria.registrar("TESTE_SISTEMA", "detalhe");

        final AuditoriaEntity registro = registroDe("TESTE_SISTEMA");
        assertThat(registro.getUsuarioId()).isNull();
        assertThat(registro.getUsuarioEmail()).isEqualTo("sistema");
    }
}
