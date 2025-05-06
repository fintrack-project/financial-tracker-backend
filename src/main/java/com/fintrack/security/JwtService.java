package com.fintrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String SECRET_KEY = "your-secret-key"; // Replace with a secure key

    private final SecretKey signingKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration; // JWT expiration time

    public JwtService() {
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
    }

    public String generateVerificationToken(String stringInput) {
      return Jwts.builder()
              .setSubject(stringInput)
              .setIssuedAt(new Date())
              .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
              // .signWith(SignatureAlgorithm.HS512, jwtSecret)
              .signWith(signingKey)
              .compact();
  }

    public String decodeToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject(); // The object is stored as the subject in the token
    }
}