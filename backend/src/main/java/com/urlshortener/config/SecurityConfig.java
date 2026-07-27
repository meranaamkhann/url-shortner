package com.urlshortener.config;

import com.urlshortener.security.AccessDeniedHandlerImpl;
import com.urlshortener.security.AuthEntryPoint;
import com.urlshortener.security.JwtAuthenticationFilter;
import com.urlshortener.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AuthEntryPoint authEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder)
            throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // see class javadoc: not applicable to stateless Bearer-token auth
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .headers(headers -> headers
                    .contentTypeOptions(opts -> {})                              // X-Content-Type-Options: nosniff
                    .frameOptions(opts -> opts.deny())                           // X-Frame-Options: DENY (clickjacking)
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000))                          // Strict-Transport-Security
                    .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://unpkg.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com data:; " +
                    "img-src 'self' data: blob:; " +
                    "connect-src 'self'; " +
                    "frame-ancestors 'none'; " +
                    "object-src 'none';"
                ))
                    .permissionsPolicy(pp -> pp.policy("geolocation=(), microphone=(), camera=()")))
            .authorizeHttpRequests(auth -> auth
                    // Public, unauthenticated endpoints
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/urls").permitAll() // anonymous shortening — see UrlController#create's javadoc
                    .requestMatchers("/r/**").permitAll()                       // the redirect hot path
                    .requestMatchers("/api/v1/public/**").permitAll()
                    .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/", "/*.html", "/assets/**", "/static/**").permitAll() // the static frontend
                    .requestMatchers("/error").permitAll()
                    // Admin-only surface
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    // Everything else requires authentication; per-resource ownership is
                    // enforced with @PreAuthorize at the service/controller method level.
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // In production this list comes from an environment variable / config map per
        // deployment environment — never "*" once credentials/Authorization headers are involved.
        config.setAllowedOriginPatterns(List.of("https://*.yourdomain.com", "http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}