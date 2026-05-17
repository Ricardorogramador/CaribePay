package ricardo.estudio.caribepay.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generarToken(String email) {
        log.debug("Generando token JWT para: {}", email);

        try {
            String token = Jwts.builder()
                    .subject(email)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(getSigningKey())
                    .compact();

            log.debug("Token generado exitosamente para: {}", email);
            return token;
        } catch (Exception e) {
            log.error("Error al generar token para {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error generando token", e);
        }
    }

    public String obtenerEmailDelToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (JwtException e) {
            log.warn("Error al extraer email del token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            log.debug("Token validado exitosamente");
            return true;
        } catch (JwtException e) {
            log.warn("Token inválido o expirado: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token vacío o nulo: {}", e.getMessage());
            return false;
        }
    }
}