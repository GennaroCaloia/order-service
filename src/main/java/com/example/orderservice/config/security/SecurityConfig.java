package com.example.orderservice.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // abilita @PreAuthorize sui metodi
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Microservizio stateless — nessuna sessione HTTP
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disabilita CSRF (inutile per API REST con JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Disabilita form login e basic auth
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // Regole di autorizzazione
                .authorizeHttpRequests(auth -> auth

                        // Endpoint pubblici — no auth richiesta
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**"
                        ).permitAll()

                        // Solo ADMIN può leggere ordini di qualsiasi customer
                        // La restrizione per CUSTOMER è applicata
                        // a livello di service con @PreAuthorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders")
                        .hasAnyRole("ADMIN", "CUSTOMER")

                        // Tutte le altre route richiedono autenticazione
                        .anyRequest().authenticated()
                )

                // Inserisce il filtro JWT prima del filtro standard
                // di Spring Security per username/password
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}