package com.example.demo.config;

import com.example.demo.service.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1) Disable CSRF since this is a stateless REST API
            .csrf(csrf -> csrf.disable())

            // 2) Enable CORS with our configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3) Stateless session (no HTTP session will be created or used)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4) Authorization rules
            .authorizeHttpRequests(auth -> auth
                // a) Public endpoints (no JWT required)
                .requestMatchers(
                    "/auth/**",               // Signup, login, forgot/reset password
                    "/users/change-password", // Change password (after login)
                    "/error"                  // Default Spring error endpoint
                ).permitAll()

                // b) Super‐Admin–only endpoints
                .requestMatchers("/api/superadmin/**").hasAuthority("SUPER_ADMIN")
                
                // Add new admin endpoints security
                .requestMatchers("/api/admin/**").hasAuthority("SUPER_ADMIN")

                .requestMatchers("/api/auditor/**").hasAnyAuthority("AUDITOR", "SUPER_ADMIN")

                // c) Customer Support endpoints
                .requestMatchers("/api/support/tickets/**").hasAuthority("CUSTOMER_SUPPORT")

                .requestMatchers("/api/loans/**")
                .hasAnyAuthority("LOAN_OFFICER", "SUPER_ADMIN")

                // d) All other requests require a valid JWT
                .anyRequest().authenticated()
            )

            // 5) Add the JWT filter BEFORE UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow only your frontend origins:
        config.setAllowedOrigins(List.of(
            "http://localhost:8000",
            "http://localhost:5500",
            "http://127.0.0.1:5500"
        ));

        // Allow standard HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow the Authorization header (for Bearer <token>) and Content-Type
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this CORS config to all endpoints
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Expose the default AuthenticationManager built from AuthenticationConfiguration
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt to hash and verify passwords
        return new BCryptPasswordEncoder();
    }
}