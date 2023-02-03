package com.david.ds.teles.apigateway.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthManager implements ReactiveAuthenticationManager {

	private final AuthUserDetailService userDetailService;

	private final JwtService jwtService;

	@Override
	public Mono<Authentication> authenticate(Authentication authentication) {
		if (authentication == null)
			return Mono.error(new BadCredentialsException("Bad credentials"));

		if (authentication.isAuthenticated())
			return Mono.just(authentication);

		final String token = (String) authentication.getCredentials();

		return jwtService.isTokenValid(token)
				.switchIfEmpty(Mono.error(new BadCredentialsException("Bad credentials")))
				.map(user -> UsernamePasswordAuthenticationToken.authenticated(user, token, user.getAuthorities()))
				.flatMap(auth -> Mono.just(auth));
	}
}
