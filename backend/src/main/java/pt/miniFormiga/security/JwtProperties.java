package pt.miniFormiga.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mini-formiga.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
