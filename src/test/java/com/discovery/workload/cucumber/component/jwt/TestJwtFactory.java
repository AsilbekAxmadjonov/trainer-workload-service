package com.discovery.workload.cucumber.component.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestJwtFactory {

    private static final String SECRET =
            "mySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm123456";

    public static String userToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject("component-user")
                .claim("roles", List.of("USER"))
                .signWith(key)
                .compact();
    }
}