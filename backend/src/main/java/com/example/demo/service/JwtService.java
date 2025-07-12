package com.example.demo.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    // 256-bit key in HEX
    private static final String SECRET_KEY = 
        "6B5970337336763979244226452948404D635166546A576E5A7234753778217A";

    // Helper: decode hex string to byte[]
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) (
                (Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16)
            );
        }
        return data;
    }

    // Build the signing key from hex
    private SecretKey getSignInKey() {
        byte[] keyBytes = hexStringToByteArray(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(getSignInKey())
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String,Object> claims = new HashMap<>();
        String role = userDetails.getAuthorities().stream()
                         .findFirst()
                         .map(a -> a.getAuthority())
                         .orElse("ROLE_USER");
        claims.put("role", role);
        return buildToken(claims, userDetails.getUsername());
    }

    private String buildToken(Map<String,Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                   .setClaims(claims)
                   .setSubject(subject)
                   .setIssuedAt(new Date(now))
                   .setExpiration(new Date(now + 1000 * 60 * 5))  // 5 min
                   .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                   .compact();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }
}
