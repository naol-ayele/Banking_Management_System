package com.BMS.Bank_Management_System.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AtmApiKeyFilter atmApiKeyFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, AtmApiKeyFilter atmApiKeyFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.atmApiKeyFilter = atmApiKeyFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*","http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/chat-test.html", "/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .requestMatchers("/ws/**", "/topic/**", "/app/**").permitAll()
                        .requestMatchers("*.html", "*.css", "*.js", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.ico").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()

                        // ATM and cardless processing endpoints - authenticated via API key
                        .requestMatchers("/api/atm/**").authenticated()
                        .requestMatchers("/api/cardless/withdrawal/process").authenticated()
                        .requestMatchers("/api/cardless/withdrawal/validate/**").authenticated()

                        // Cardless withdrawal request endpoints - for customers only
                        .requestMatchers("/api/cardless/withdrawal/request").hasRole("CUSTOMER")
                        .requestMatchers("/api/cardless/withdrawal/qr/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/cardless/withdrawal/details/**").hasRole("CUSTOMER")

                        // Customer loan endpoints
                        .requestMatchers("/api/loans/**").hasRole("CUSTOMER")

                        // Loan officer endpoints (admin approval/rejection)
                        .requestMatchers("/api/admin/loans/**").hasRole("LOAN_OFFICER")

                        // All other endpoints require authentication
                        .requestMatchers("/analytics/**").permitAll()

                        .anyRequest().authenticated())
                .headers(AbstractHttpConfigurer::disable)
                .with(new CorsConfigurer<>(), Customizer.withDefaults())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .addFilterBefore(atmApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}