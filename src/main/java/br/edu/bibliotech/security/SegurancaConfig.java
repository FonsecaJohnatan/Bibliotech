package br.edu.bibliotech.security;

import br.edu.bibliotech.model.Usuario;
import br.edu.bibliotech.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Locale;

@Configuration
public class SegurancaConfig {

    @Bean
    public BCryptPasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService usuariosAutenticados(UsuarioRepository usuarios) {
        return email -> {
            String emailNormalizado = email.toLowerCase(Locale.ROOT);
            Usuario usuario = usuarios.findByEmail(emailNormalizado)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

            return User.withUsername(usuario.getEmail())
                    .password(usuario.getSenha())
                    .roles(usuario.getPerfil().name())
                    .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(regras -> regras
                .requestMatchers("/css/**", "/login", "/erro").permitAll()
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/service/usuarios/**", "/service/emprestimos/**")
                .hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.GET, "/service/**").permitAll()
                .requestMatchers("/service/**").hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.GET, "/autor/**", "/categoria/**", "/exemplar/**",
                        "/emprestimo/**", "/usuario/**", "/livro/formulario", "/livro/*/editar")
                .hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers(HttpMethod.POST, "/autor/**", "/categoria/**", "/exemplar/**",
                        "/emprestimo/**", "/usuario/**", "/livro/**")
                .hasAnyRole("ADMIN", "BIBLIOTECARIO")
                .requestMatchers("/", "/home", "/livro", "/livro/*").permitAll()
                .requestMatchers("/meus-emprestimos").authenticated()
                .anyRequest().authenticated());

        http.formLogin(login -> login.loginPage("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/home", false)
                .permitAll());

        http.httpBasic(Customizer.withDefaults());

        http.logout(saida -> saida.logoutUrl("/sair")
                .logoutSuccessUrl("/login?logout")
                .permitAll());

        http.csrf(csrf -> csrf.ignoringRequestMatchers("/service/**", "/h2-console/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
