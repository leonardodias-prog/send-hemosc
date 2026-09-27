package br.univille.sendhemosc.config;

import br.univille.sendhemosc.domain.enums.PerfilUsuario;
import br.univille.sendhemosc.domain.port.outbound.IUsuarioRepositoryPort;
import br.univille.sendhemosc.usecase.usuario.LimiteDeTentativasDeLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso do sistema.
 *
 * <p>A separacao segue o efeito de cada acao. Consultar e alimentar dados fica com qualquer
 * funcionario autenticado. Disparar convocacao e ligar o envio real ficam restritos ao
 * responsavel, porque mandam e-mail para pessoas de verdade e isso nao se desfaz. Gerenciar
 * contas fica com o administrador.</p>
 */
@Configuration
@RequiredArgsConstructor
public class SegurancaConfig {

    private static final String PERFIL_OPERADOR = PerfilUsuario.OPERADOR.name();
    private static final String PERFIL_RESPONSAVEL = PerfilUsuario.RESPONSAVEL.name();
    private static final String PERFIL_MASTER = PerfilUsuario.MASTER.name();

    private final IUsuarioRepositoryPort usuarioRepository;
    private final LimiteDeTentativasDeLogin limiteDeTentativas;

    /**
     * Fator de custo acima do padrao do Spring, que e 10. A base e pequena e o login e
     * esporadico, entao o custo extra nao pesa e dificulta ataque de forca bruta.
     *
     * @return codificador de senha
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Registra o momento do acesso apos autenticacao bem sucedida, e esquece as falhas anteriores
     * do e-mail no limite de tentativas.
     *
     * @return tratador de sucesso
     */
    @Bean
    public AuthenticationSuccessHandler sucessoAoEntrar() {
        return (requisicao, resposta, autenticacao) -> {
            limiteDeTentativas.liberar(autenticacao.getName());
            usuarioRepository.registrarAcesso(autenticacao.getName());
            resposta.sendRedirect(requisicao.getContextPath() + "/");
        };
    }

    @Bean
    public SecurityFilterChain filtros(final HttpSecurity http,
                                       final AuthenticationSuccessHandler sucessoAoEntrar,
                                       final FalhaAoEntrar falhaAoEntrar) throws Exception {
        http
                .authorizeHttpRequests(regras -> regras
                    // Publico: entrar, cadastrar-se, recuperar a senha, termo de uso, descadastro
                    // do doador e saude. /meus-dados/** e do doador, que nao tem conta: ele se
                    // identifica pelo token do link dos e-mails, o mesmo do descadastro.
                    .requestMatchers("/login", "/cadastro", "/senha/esqueci", "/senha/redefinir/**", "/termo",
                            "/descadastro/**", "/meus-dados/**", "/aprovacao/**",
                            "/actuator/health", "/actuator/info", "/css/**").permitAll()
                    // Gerenciamento de contas
                    .requestMatchers("/usuarios/**").hasRole(PERFIL_MASTER)
                    // Liberar o limite de contato de alguem e decisao do administrador. Antes de
                    // /doadores/**, pela mesma razao das acoes de envio logo abaixo.
                    .requestMatchers("/doadores/*/liberar-contato").hasRole(PERFIL_MASTER)
                    // Excluir apaga tambem doacoes e convocacoes e nao tem volta: fica com quem
                    // responde pela captacao. O operador edita e desativa, que se desfaz.
                    .requestMatchers("/doadores/*/excluir").hasAnyRole(PERFIL_RESPONSAVEL, PERFIL_MASTER)
                    // Acoes que produzem e-mail de verdade. Precisam vir antes das regras
                    // gerais abaixo: a primeira correspondencia decide, e /doadores/** liberaria
                    // a convocacao seletiva para quem so deveria alimentar dados.
                    .requestMatchers("/convocar/**", "/api/convocacoes/**", "/envio/**",
                            "/doadores/convocar", "/doadores/*/convocar", "/disparo-automatico/**")
                            .hasAnyRole(PERFIL_RESPONSAVEL, PERFIL_MASTER)
                    // Alimentacao de dados e consulta
                    .requestMatchers("/", "/doadores/**", "/estoque/**", "/api/**")
                            .hasAnyRole(PERFIL_OPERADOR, PERFIL_RESPONSAVEL, PERFIL_MASTER)
                    .anyRequest().authenticated())
                .formLogin(login -> login
                    .loginPage("/login")
                    .usernameParameter("email")
                    .passwordParameter("senha")
                    .successHandler(sucessoAoEntrar)
                    .failureHandler(falhaAoEntrar)
                    .permitAll())
                // Antes da conferencia da senha: e-mail bloqueado nao chega a ela.
                .addFilterBefore(new BloqueioDeLoginFilter(limiteDeTentativas), UsernamePasswordAuthenticationFilter.class)
                .logout(saida -> saida
                    .logoutUrl("/sair")
                    .logoutSuccessUrl("/login?saiu")
                    .permitAll())
                // O console do H2 usa quadros e tem protecao propria desativada; fica restrito ao
                // perfil de desenvolvimento, onde a linha abaixo nao expoe ambiente publicado.
                .headers(cabecalhos -> cabecalhos.frameOptions(quadros -> quadros.sameOrigin()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"));

        return http.build();
    }
}
