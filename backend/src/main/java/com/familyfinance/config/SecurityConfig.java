package com.familyfinance.config;

import com.familyfinance.service.LedgerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Value("${FRONTEND_ORIGIN:http://localhost:5173,http://localhost:5174}") private String frontendOrigin;
    @Value("${app.auth.father-password:dev-father-change-me}") private String fatherPassword;
    @Value("${app.auth.son1-password:dev-son1-change-me}") private String son1Password;
    @Value("${app.auth.son2-password:dev-son2-change-me}") private String son2Password;

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(LedgerService ledger, PasswordEncoder encoder) {
        var father = ledger.userByUsername("father"); var son1 = ledger.userByUsername("son1"); var son2 = ledger.userByUsername("son2");
        return new InMemoryUserDetailsManager(
            User.withUsername(father.username()).password(encoder.encode(fatherPassword)).roles("ADMIN").build(),
            User.withUsername(son1.username()).password(encoder.encode(son1Password)).roles("MEMBER").build(),
            User.withUsername(son2.username()).password(encoder.encode(son2Password)).roles("MEMBER").build());
    }

    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(); provider.setUserDetailsService(users); provider.setPasswordEncoder(encoder); return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository repository) throws Exception {
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse(); var handler = new CsrfTokenRequestAttributeHandler();
        http.securityContext(context -> context.securityContextRepository(repository))
            .csrf(config -> config.csrfTokenRepository(csrf).csrfTokenRequestHandler(handler))
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth.requestMatchers("/ping", "/api/v1/auth/**", "/actuator/health", "/error").permitAll().anyRequest().authenticated())
            .logout(logout -> logout.logoutUrl("/api/v1/auth/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID", "XSRF-TOKEN"));
        return http.build();
    }

    @Bean CorsConfigurationSource corsConfigurationSource() { var config = new CorsConfiguration(); config.setAllowedOriginPatterns(List.of(frontendOrigin.split(","))); config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS")); config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN")); config.setAllowCredentials(true); var source = new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**", config); return source; }
}
