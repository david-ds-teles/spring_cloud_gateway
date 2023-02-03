package com.david.ds.teles.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.WebFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final GlobalCorsProperties gatewayGlobalCorsProperties;

	private final AuthManager authManager;

	@Value("${spring.cloud.gateway.permitAll}")
	private String[] whitelistedEndpoints;

	@Bean
	public SecurityWebFilterChain securityConfiguration(ServerHttpSecurity http) {
		return http
				.cors(this::configSecurityCors)
				.csrf().disable()
				.httpBasic().disable()
				.formLogin().disable()
				.authorizeExchange(exchange -> {
					exchange.pathMatchers(whitelistedEndpoints).permitAll();
					exchange.anyExchange().authenticated();
				})
				.addFilterAt(securityFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
				.build();
	}

	private WebFilter securityFilter() {
		final HttpStatusServerEntryPoint authenticationEntryPoint = new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED);
		final ServerAuthenticationFailureHandler failureHandler = new ServerAuthenticationEntryPointFailureHandler(authenticationEntryPoint);
		final ServerAuthenticationSuccessHandler successHandler = new WebFilterChainServerAuthenticationSuccessHandler();

		return new SecurityFilter(authManager, whitelistedEndpoints, failureHandler, successHandler);
	}

	/**
	 * Using the same gateway proxy configuration for the security filter chain.
	 * @param customizer
	 */
	private void configSecurityCors(ServerHttpSecurity.CorsSpec customizer) {
		CorsConfiguration gatewayCorsConfig = this.gatewayGlobalCorsProperties.getCorsConfigurations().get("/**");

		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.setAllowedOrigins(gatewayCorsConfig.getAllowedOrigins());
		corsConfiguration.setAllowedHeaders(gatewayCorsConfig.getAllowedHeaders());
		corsConfiguration.setAllowedMethods(gatewayCorsConfig.getAllowedMethods());
		corsConfiguration.setAllowCredentials(gatewayCorsConfig.getAllowCredentials());
		customizer.configurationSource(request -> corsConfiguration);
	}
}
