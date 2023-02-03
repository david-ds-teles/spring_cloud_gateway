package com.david.ds.teles.apigateway.security;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import reactor.core.publisher.Mono;

@Service
public class JwtService {

	@Value("${spring.security.secretKey}")
	private String secretKey;

	public Mono<String> generateToken(String userName) {
		final Set<String> roles = Set.of("USER");

		final Date expireAt = Date.from(LocalDateTime.now().plus(1, ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toInstant());
		final String token = Jwts.builder()
				.setSubject(userName)
				.setExpiration(expireAt)
				.claim("roles", roles)
				.signWith(SignatureAlgorithm.HS512, secretKey.getBytes())
				.compact();

		return Mono.just(token);
	}

	public Mono<UserDetails> isTokenValid(String token) {
		if (!StringUtils.hasText(token))
			return Mono.empty();

		Claims claims = Jwts.parser()
				.setSigningKey(Base64.getEncoder().encode(secretKey.getBytes()))
				.parseClaimsJws(token)
				.getBody();

		final LocalDateTime expiration = claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

		if (expiration.isBefore(LocalDateTime.now()))
			return Mono.empty();

		final String userName = claims.getSubject();
		final List<String> roles = claims.get("roles", ArrayList.class);

		UserDetails user = User.builder()
				.username(userName)
				.password(token)
				.authorities(roles.stream().reduce((a,b) -> a + "," + b).get())
				.build();

		return Mono.just(user);
	}

}
