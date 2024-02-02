package dev.mpapenbr.webflux.demo.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

@Configuration
@EnableWebFlux
@EnableWebFluxSecurity
public class SecurityConfig {
	private final ApplicationContext applicationContext;

	public SecurityConfig(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	MapReactiveUserDetailsService userDetailsService() {
		return new MapReactiveUserDetailsService(
				User.builder().username("user").password("{noop}password").roles("USER").build(),
				User.builder().username("actuator").password("{noop}act").roles("ACTUATOR").build());
	}

	@Bean
	@Order(1)
	SecurityWebFilterChain actuatorFilterChain(ServerHttpSecurity http, CustomFilterA customFilterA) {
		return http.securityMatcher(ServerWebExchangeMatchers.pathMatchers("/actuator/**"))
				.authorizeExchange(a -> a.pathMatchers("/actuator/health").permitAll().anyExchange().authenticated())
				.httpBasic(Customizer.withDefaults())
				.addFilterAfter(customFilterA, SecurityWebFiltersOrder.AUTHENTICATION).build();
	}

	@Bean
	@Order(2)
	SecurityWebFilterChain mainFilterChain(ServerHttpSecurity http, CustomFilterB customFilterB) {
		return http.authorizeExchange(a -> a.anyExchange().authenticated()).httpBasic(Customizer.withDefaults())
				.addFilterAfter(customFilterB, SecurityWebFiltersOrder.AUTHENTICATION).build();
	}

	@Bean
	Consumer<List<WebFilter>> globalFilterCustomizer() {
		Predicate<WebFilter> isCustomFilter = f -> f.getClass().getName().contains("CustomFilter");

		return filters -> filters.removeIf(isCustomFilter);

	}

	@Bean
	public HttpHandler httpHandler(ObjectProvider<WebFluxProperties> propsProvider,
			Consumer<List<WebFilter>> globalFilterCustomizer) {
		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(this.applicationContext)
				.filters(globalFilterCustomizer).build();
		WebFluxProperties properties = propsProvider.getIfAvailable();
		if (properties != null && StringUtils.hasText(properties.getBasePath())) {
			Map<String, HttpHandler> handlersMap = Collections.singletonMap(properties.getBasePath(), httpHandler);
			return new ContextPathCompositeHandler(handlersMap);
		}
		return httpHandler;
	}
}
