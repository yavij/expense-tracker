package expensetracker.util;

import expensetracker.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_ENV = "JWT_SECRET";
    private static final long EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private static SecretKey getKey() {
        String secret = System.getenv(SECRET_ENV);
        if (secret == null || secret.length() < 32) {
            secret = "default-secret-change-in-production-min-32-chars";
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String createToken(User user) {
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getKey())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String getUserId(Claims claims) {
        return claims.getSubject();
    }

    public static String getRole(Claims claims) {
        return (String) claims.get("role");
    }
}
