package com.discovery.workload.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    // HS256 needs a sufficiently long secret (>= 32 bytes recommended)
    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final TokenService tokenService = new TokenService(SECRET);

    private String createJwt(String subject, Object rolesClaim) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        // build claims
        Map<String, Object> claims = new java.util.HashMap<>();
        if (rolesClaim != null) {
            claims.put("roles", rolesClaim);
        }

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .signWith(key) // jjwt picks the right algo for HMAC key
                .compact();
    }

    @Test
    void parseClaims_validJwt_returnsClaims() {
        String jwt = createJwt("john", List.of("ROLE_USER", "ROLE_ADMIN"));

        Claims claims = tokenService.parseClaims(jwt);

        assertEquals("john", claims.getSubject());
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), claims.get("roles"));
    }

    @Test
    void getUsername_returnsSubject() {
        String jwt = createJwt("asilbek", List.of("ROLE_USER"));

        assertEquals("asilbek", tokenService.getUsername(jwt));
    }

    @Test
    void getRoles_rolesIsList_returnsList() {
        String jwt = createJwt("john", List.of("ROLE_USER", "ROLE_ADMIN"));

        List<String> roles = tokenService.getRoles(jwt);

        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), roles);
    }

    @Test
    void getRoles_rolesMissing_returnsEmptyList() {
        String jwt = createJwt("john", null);

        List<String> roles = tokenService.getRoles(jwt);

        assertEquals(List.of(), roles);
    }

    @Test
    void getRoles_rolesIsNotList_returnsEmptyList() {
        // roles claim is string instead of list
        String jwt = createJwt("john", "ROLE_USER");

        List<String> roles = tokenService.getRoles(jwt);

        assertEquals(List.of(), roles);
    }

    @Test
    void parseClaims_wrongSecret_throws() {
        String jwtSignedWithOtherSecret = Jwts.builder()
                .subject("john")
                .claim("roles", List.of("ROLE_USER"))
                .signWith(Keys.hmacShaKeyFor(
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                .getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(Exception.class, () -> tokenService.parseClaims(jwtSignedWithOtherSecret));
    }
}
