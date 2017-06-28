package com.example.reactive.health;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveCompositeHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
public class ReactiveCompositeHealthIndicatorTests {

	private OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();

	@Test
	public void singleIndicator() {
		ReactiveHealthIndicator indicator = new ReactiveCompositeHealthIndicator(
				this.healthAggregator,
				Collections.singletonMap("test", () -> Mono.just(Health.up().build())));

		StepVerifier.create(indicator.health()).consumeNextWith(h -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("test");
			assertThat(h.getDetails().get("test")).isEqualTo(Health.up().build());
		}).verifyComplete();
	}

	@Test
	public void longHealth() {
		Map<String, ReactiveHealthIndicator> indicators = new HashMap<>();
		for (int i = 0; i < 50; i++) {
			indicators.put("test" + i, new TimeoutHealth(10000, Status.UP));
		}
		ReactiveHealthIndicator indicator = new ReactiveCompositeHealthIndicator(
				this.healthAggregator, indicators);
		StepVerifier.withVirtualTime(indicator::health)
				.expectSubscription()
				.thenAwait(Duration.ofMillis(10000))
				.consumeNextWith(h -> {
					assertThat(h.getStatus()).isEqualTo(Status.UP);
					assertThat(h.getDetails()).hasSize(50);
				})
				.verifyComplete();

	}

}
