package br.univille.sendhemosc.usecase.usuario;

import br.univille.sendhemosc.domain.dto.FotoDoUsuario;
import br.univille.sendhemosc.domain.dto.PerfilDaConta;
import br.univille.sendhemosc.domain.dto.UsuarioAutenticavel;
import br.univille.sendhemosc.domain.dto.UsuarioResumo;
import br.univille.sendhemosc.domain.exception.NegocioException;
import br.univille.sendhemosc.domain.exception.UsuarioErrorsMessage;
import br.univille.sendhemosc.domain.port.outbound.IAuditoriaPort;
import br.univille.sendhemosc.domain.port.outbound.IFotoUsuarioRepositoryPort;
import br.univille.sendhemosc.domain.port.outbound.IRecuperacaoSenhaPort;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * O perfil da propria pessoa: ver os dados da conta, corrigir nome e e-mail, e escolher a foto.
 *
 * <p>So nome e e-mail podem ser mudados daqui. Perfil de acesso e situacao da conta sao do
 * administrador, e nenhum caminho deste caso de uso chega neles.</p>
 *
 * <p>Trocar o e-mail pede a senha atual, mesmo de quem ja entrou, pelo motivo que ja vale para a
 * troca de senha: sessao aberta num computador esquecido nao pode bastar para tomar a conta. O
 * e-mail e o destino do link de recuperacao de senha, entao quem o controla controla a conta.</p>
 */
@Service
@RequiredArgsConstructor
public class GerenciarPerfilUseCase {

    private static final int NOME_MAXIMO = 150;
    private static final int EMAIL_MAXIMO = 180;
    /** A coluna de detalhe da auditoria tem 500 posicoes, e a gravacao nao corta o que passa. */
    private static final int DETALHE_MAXIMO = 500;
    private static final Pattern FORMATO_DO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final IUsuarioRepositoryPort usuarioRepository;
    private final IFotoUsuarioRepositoryPort fotoRepository;
    private final IRecuperacaoSenhaPort recuperacaoSenha;
    private final IAuditoriaPort auditoria;
    private final PasswordEncoder passwordEncoder;

    /**
     * Reune o que a tela de perfil mostra.
     *
     * @param email e-mail da conta autenticada
     * @return os dados da conta e se ela tem foto
     */
    public PerfilDaConta consultar(final String email) {
        final UsuarioAutenticavel conta = exigirConta(email);
        final UsuarioResumo usuario = usuarioRepository.buscarPorId(conta.id())
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        return new PerfilDaConta(usuario, fotoRepository.momentoDaFoto(conta.id()).isPresent());
    }

    /**
     * Corrige nome e e-mail da propria conta.
     *
     * @param emailAtual e-mail da conta autenticada
     * @param nome nome como a pessoa quer que apareca
     * @param novoEmail e-mail desejado, igual ao atual quando nao muda
     * @param senhaAtual senha em uso, conferida somente quando o e-mail muda
     * @return o que de fato mudou
     */
    public Resultado alterarDados(final String emailAtual, final String nome,
                                  final String novoEmail, final String senhaAtual) {
        final UsuarioAutenticavel conta = exigirConta(emailAtual);
        final String nomeLimpo = nome == null ? "" : nome.trim();
        final String emailLimpo = novoEmail == null ? "" : novoEmail.trim().toLowerCase(Locale.ROOT);

        validarNome(nomeLimpo);
        validarEmail(emailLimpo);

        final boolean nomeMudou = !nomeLimpo.equals(conta.nome());
        final boolean emailMudou = !emailLimpo.equalsIgnoreCase(conta.email());

        if (emailMudou) {
            exigirSenhaAtual(conta, senhaAtual);

            if (usuarioRepository.existeComEmailDeOutro(emailLimpo, conta.id())) {
                throw new NegocioException(UsuarioErrorsMessage.EMAIL_DUPLICADO);
            }
        }
        if (!nomeMudou && !emailMudou) {
            return new Resultado(false, false);
        }

        usuarioRepository.atualizarDadosPessoais(conta.id(), nomeLimpo, emailMudou ? emailLimpo : conta.email())
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));

        if (emailMudou) {
            // Link pedido antes seguiu para o endereco antigo, que deixou de ser o da conta.
            recuperacaoSenha.invalidarAbertos(conta.id(), LocalDateTime.now());
        }

        auditoria.registrar("PERFIL_ALTERADO", descreverMudancas(conta, nomeLimpo, emailLimpo, nomeMudou, emailMudou));

        return new Resultado(nomeMudou, emailMudou);
    }

    /**
     * Guarda a foto enviada, no lugar da anterior.
     *
     * @param email e-mail da conta autenticada
     * @param arquivo bytes do arquivo enviado, de qualquer tamanho que o servidor tenha aceito
     */
    public void trocarFoto(final String email, final byte[] arquivo) {
        final UsuarioAutenticavel conta = exigirConta(email);
        final byte[] foto = ProcessadorDeFoto.processar(arquivo);

        fotoRepository.salvar(conta.id(), ProcessadorDeFoto.TIPO_GRAVADO, foto, LocalDateTime.now());
    }

    /**
     * Tira a foto, de modo que a tela volte a mostrar a inicial do nome.
     *
     * @param email e-mail da conta autenticada
     * @return true se havia foto para tirar
     */
    public boolean removerFoto(final String email) {
        return fotoRepository.remover(exigirConta(email).id());
    }

    /**
     * Entrega a foto da propria conta. Nao existe versao que receba o identificador de outra
     * conta: a foto de alguem so e entregue a ele mesmo.
     *
     * @param email e-mail da conta autenticada
     * @return a foto, vazio quando a conta nao tem
     */
    public Optional<FotoDoUsuario> buscarFoto(final String email) {
        return fotoRepository.buscar(exigirConta(email).id());
    }

    private UsuarioAutenticavel exigirConta(final String email) {
        return usuarioRepository.buscarPorEmail(email)
                .orElseThrow(() -> new NegocioException(UsuarioErrorsMessage.NAO_ENCONTRADO));
    }

    private void exigirSenhaAtual(final UsuarioAutenticavel conta, final String senhaAtual) {
        if (senhaAtual == null || RegraDeSenha.excedeOLimite(senhaAtual)
                || !passwordEncoder.matches(senhaAtual, conta.senhaHash())) {
            throw new NegocioException(UsuarioErrorsMessage.SENHA_ATUAL_INCORRETA);
        }
    }

    private void validarNome(final String nome) {
        if (nome.isEmpty() || nome.length() > NOME_MAXIMO || temCaractereDeControle(nome)) {
            throw new NegocioException(UsuarioErrorsMessage.NOME_INVALIDO);
        }
    }

    private void validarEmail(final String email) {
        if (email.length() > EMAIL_MAXIMO || temCaractereDeControle(email)
                || !FORMATO_DO_EMAIL.matcher(email).matches()) {
            throw new NegocioException(UsuarioErrorsMessage.EMAIL_INVALIDO);
        }
    }

    /** Quebra de linha no nome desmontaria o registro de auditoria e a tela de contas. */
    private boolean temCaractereDeControle(final String texto) {
        return texto.chars().anyMatch(Character::isISOControl);
    }

    private String descreverMudancas(final UsuarioAutenticavel conta, final String nomeNovo, final String emailNovo,
                                     final boolean nomeMudou, final boolean emailMudou) {
        final List<String> mudancas = new ArrayList<>();

        if (nomeMudou) {
            mudancas.add("nome de \"%s\" para \"%s\"".formatted(conta.nome(), nomeNovo));
        }
        if (emailMudou) {
            mudancas.add("e-mail de %s para %s".formatted(conta.email(), emailNovo));
        }

        final String texto = String.join("; ", mudancas);

        return texto.length() <= DETALHE_MAXIMO ? texto : texto.substring(0, DETALHE_MAXIMO - 1) + "…";
    }

    /**
     * Desfecho da correcao de dados.
     *
     * @param nomeMudou true quando o nome foi alterado
     * @param emailMudou true quando o e-mail foi alterado, o que obriga a pessoa a entrar de novo
     */
    public record Resultado(boolean nomeMudou, boolean emailMudou) {

        /**
         * Indica se algo foi gravado.
         *
         * @return false quando a pessoa salvou sem mexer em nada
         */
        public boolean mudouAlgo() {
            return nomeMudou || emailMudou;
        }
    }
}
