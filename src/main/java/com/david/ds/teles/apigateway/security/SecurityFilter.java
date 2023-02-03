package com.david.ds.teles.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class SecurityFilter implements WebFilter {

	private final AuthManager authManager;

	private final String[] whitelistedPaths;

	private final ServerAuthenticationFailureHandler authenticationFailureHandler;

	private final ServerAuthenticationSuccessHandler authenticationSuccessHandler;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return ServerWebExchangeMatchers.pathMatchers(whitelistedPaths)
				.matches(exchange)
				.filter((matchResult) -> !matchResult.isMatch())
				.flatMap((matchResult) -> extractCredentials(exchange))
				.switchIfEmpty(Mono.defer(() -> chain.filter(exchange).then(Mono.empty())))
				.flatMap(authManager::authenticate)
				.flatMap((auth) -> onAuthenticationSuccess(auth, new WebFilterExchange(exchange, chain)))
				.onErrorResume(AuthenticationException.class, (ex) -> this.onAuthenticationFailure(exchange, chain, ex));
	}

	private Mono<Authentication> extractCredentials(ServerWebExchange exchange) {
		final ServerHttpRequest request = exchange.getRequest();
		final String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

		if (!StringUtils.hasText(authorization) || "null".equals(authorization)) {
			log.warn("Authorization header is missing or null");
			return Mono.empty();
		}

		final Authentication credentials = UsernamePasswordAuthenticationToken.unauthenticated(null, authorization);
		return Mono.just(credentials);
	}

	private Mono<Void> onAuthenticationSuccess(Authentication authentication, WebFilterExchange webFilterExchange) {
		SecurityContextImpl securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(authentication);
		return this.authenticationSuccessHandler.onAuthenticationSuccess(webFilterExchange, authentication)
				.contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
	}

	private Mono<Void> onAuthenticationFailure(ServerWebExchange exchange, WebFilterChain chain, AuthenticationException ex) {
		final WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);
		return this.authenticationFailureHandler.onAuthenticationFailure(webFilterExchange, ex);
	}
}
