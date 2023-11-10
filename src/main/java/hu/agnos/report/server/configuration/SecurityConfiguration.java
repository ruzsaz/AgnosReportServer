package hu.agnos.report.server.configuration;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Handles the security and cors methods
 */
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfiguration {

    @Bean
    static SecurityFilterChain filterChain(HttpSecurity http, ServerProperties serverProperties, @Value("${origins:[]}") String[] origins, @Value("${permit-all:[]}") String[] permitAll, AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver) throws Exception {

        http.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver));

        // Enable and configure CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource(origins)));

        // State-less session (state in access-token only)
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Disable CSRF because of state-less session-management
        http.csrf(AbstractHttpConfigurer::disable);

        // Return 401 (unauthorized) instead of 302 (redirect to log in) when authorization is missing or invalid
        http.exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, authException) -> {
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Restricted Content\"");
            response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
        }));

        // If SSL enabled, disable http (https only)
        if (serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        // @formatter:off
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(Stream.of(permitAll).map(AntPathRequestMatcher::new).toArray(AntPathRequestMatcher[]::new)).permitAll()
                .anyRequest().authenticated());
        // @formatter:on

        return http.build();
    }

    private static UrlBasedCorsConfigurationSource corsConfigurationSource(String[] origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(origins));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    static AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(IssuerProperties issuerProperties, SpringAddonsJwtAuthenticationConverter authenticationConverter) {
        Map<String, AuthenticationManager> authProviders = new HashMap<>(1);
        authProviders.put(issuerProperties.getUri().toString(), SecurityConfiguration.authenticationProvider(issuerProperties.getUri().toString(), authenticationConverter)::authenticate);
        return new JwtIssuerAuthenticationManagerResolver(authProviders::get);
    }

    private static JwtAuthenticationProvider authenticationProvider(String issuer, Converter<Jwt, JwtAuthenticationToken> authConverter) {
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        var provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(authConverter);
        return provider;
    }

    @Getter
    @Configuration
    @ConfigurationProperties(prefix = "auth-issuer")
    protected static class IssuerProperties {

        private URL uri;

        @NestedConfigurationProperty
        private ClaimMappingProperties[] claims;

        private String usernameJsonPath = JwtClaimNames.SUB;

        public void setUri(URL uri) {
            this.uri = uri;
        }

        public void setClaims(ClaimMappingProperties[] claims) {
            this.claims = claims;
        }

        public void setUsernameJsonPath(String usernameJsonPath) {
            this.usernameJsonPath = usernameJsonPath;
        }

        @Getter
        @Setter
        protected static class ClaimMappingProperties {
            private String jsonPath;
        }

    }

    protected static class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<? extends GrantedAuthority>> {

        private final IssuerProperties properties;

        protected JwtGrantedAuthoritiesConverter(IssuerProperties properties) {
            this.properties = properties;
        }

        @Override
        public Collection<? extends GrantedAuthority> convert(@NotNull Jwt jwt) {
            return Stream.of(properties.claims).flatMap(claimProperties -> {
                Object claim;
                try {
                    claim = JsonPath.read(jwt.getClaims(), claimProperties.jsonPath);
                } catch (PathNotFoundException e) {
                    claim = null;
                }
                if (claim == null) {
                    return Stream.empty();
                }
                if (claim instanceof String claimStr) {
                    return Stream.of(claimStr.split(","));
                }
                if (claim instanceof String[] claimArr) {
                    return Stream.of(claimArr);
                }
                if (Collection.class.isAssignableFrom(claim.getClass())) {
                    var iterator = ((Collection) claim).iterator();
                    if (!iterator.hasNext()) {
                        return Stream.empty();
                    }
                    var firstItem = iterator.next();
                    if (firstItem instanceof String) {
                        return (Stream<String>) ((Collection) claim).stream();
                    }
                    if (Collection.class.isAssignableFrom(firstItem.getClass())) {
                        return (Stream<String>) ((Collection) claim).stream().flatMap(colItem -> ((Collection) colItem).stream()).map(String.class::cast);
                    }
                }
                return Stream.empty();
            }).map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
        }

    }

    @Component
    protected static class SpringAddonsJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

        private final IssuerProperties issuerProperties;

        public SpringAddonsJwtAuthenticationConverter(IssuerProperties issuerProperties) {
            this.issuerProperties = issuerProperties;
        }

        @Override
        public JwtAuthenticationToken convert(@NotNull Jwt jwt) {
            final var authorities = new JwtGrantedAuthoritiesConverter(issuerProperties).convert(jwt);
            final String username = JsonPath.read(jwt.getClaims(), issuerProperties.getUsernameJsonPath());
            return new JwtAuthenticationToken(jwt, authorities, username);
        }

    }

}
