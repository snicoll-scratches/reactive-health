package com.example.reactive.health;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health;

/**
 *
 * @author Stephane Nicoll
 */
@FunctionalInterface
public interface ReactiveHealthIndicator {

	Mono<Health> health();
	
}
