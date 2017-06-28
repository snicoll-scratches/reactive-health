package com.example.reactive.health;

import java.time.Duration;

import com.example.reactive.health.ReactiveHealthIndicator;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

/**
 * Sample {@link ReactiveHealthIndicator} that applies a configurable delay.
 *
 * @author Stephane Nicoll
 */
class TimeoutHealth implements ReactiveHealthIndicator {

	private final long timeout;
	private final Status status;

	public TimeoutHealth(long timeout, Status status) {
		this.timeout = timeout;
		this.status = status;
	}

	public TimeoutHealth(long timeout) {
		this(timeout, Status.UP);
	}

	@Override
	public Mono<Health> health() {
		return Mono.delay(Duration.ofMillis(timeout))
				.map(l -> Health.status(this.status).build());
	}

}
