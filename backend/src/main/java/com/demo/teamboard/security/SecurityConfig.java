package com.demo.teamboard.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.demo.teamboard.config.TeamboardProperties;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Resource server configuration.
 *
 * <p>Note what is absent: there is no login page, no redirect to Auth0, no client secret. That half
 * of OAuth lives entirely in the React app. This service only ever receives an access token that
 * someone else obtained, and its whole job is to decide whether to trust it.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // Safe to disable: there are no cookies and no sessions, so there is nothing for a
                // cross-site request to ride on. Authorization is a bearer token the browser will
                // not attach automatically.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Builds the decoder explicitly so every check a token must survive is listed in one place:
     * signature (via the tenant's JWKS, fetched and cached from the issuer), expiry and issuer,
     * audience, and organization.
     */
    @Bean
    JwtDecoder jwtDecoder(OAuth2ResourceServerProperties resourceServer, TeamboardProperties teamboard) {
        String issuerUri = resourceServer.getJwt().getIssuerUri();
        List<String> audiences = resourceServer.getJwt().getAudiences();

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));
        audiences.forEach(audience -> validators.add(new AudienceValidator(audience)));
        validators.add(new OrgIdValidator(teamboard.allowedOrgIds()));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    /**
     * Turns the namespaced roles claim written by the Auth0 post-login Action into Spring Security
     * authorities, so {@code @PreAuthorize("hasRole('admin')")} works in controllers.
     *
     * <p>Because the user authenticated through an organization, these roles are the ones they hold
     * <em>in that organization</em> -- the same person arrives as ROLE_admin on one login and
     * ROLE_member on the next.
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(TeamboardProperties teamboard) {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList(teamboard.rolesClaim());
            if (roles != null) {
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(TeamboardProperties teamboard) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(teamboard.corsOrigin()));
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
