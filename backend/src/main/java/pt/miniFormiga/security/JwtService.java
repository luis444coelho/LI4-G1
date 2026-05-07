package pt.miniFormiga.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pt.miniFormiga.domain.Utilizador;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String emitirToken(Utilizador utilizador, UserDetails userDetails) {
        Instant agora = Instant.now();
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(utilizador.getUsername())
                .id(UUID.randomUUID().toString())
                .claim("uid", utilizador.getId().toString())
                .claim("perfil", utilizador.getPerfil().getNome())
                .claim("lojaId", utilizador.getLoja().getId().toString())
                .claim("authorities", authorities)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(properties.expirationMinutes() * 60)))
                .signWith(secretKey())
                .compact();
    }

    public String extrairUsername(String token) {
        return claims(token).getSubject();
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        return userDetails.getUsername().equals(extrairUsername(token))
                && claims(token).getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
