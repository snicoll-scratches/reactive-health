package com.example.reactive.health;

import java.time.Duration;

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

	private static final Health UNKNOWN_HEALTH = Health.unknown()
			.withDetail("detail", "value").build();

	private static final Health HEALTHY = Health.up().build();

	private OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();

	private ReactiveCompositeHealthIndicator indicator =
			new ReactiveCompositeHealthIndicator(this.healthAggregator);

	@Test
	public void singleIndicator() {
		this.indicator.addHealthIndicator("test", () -> Mono.just(HEALTHY));
		StepVerifier.create(this.indicator.health()).consumeNextWith(h -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("test");
			assertThat(h.getDetails().get("test")).isEqualTo(HEALTHY);
		}).verifyComplete();
	}

	@Test
	public void longHealth() {
		for (int i = 0; i < 50; i++) {
			this.indicator.addHealthIndicator(
					"test" + i, new TimeoutHealth(10000, Status.UP));
		}
		StepVerifier.withVirtualTime(this.indicator::health)
				.expectSubscription()
				.thenAwait(Duration.ofMillis(10000))
				.consumeNextWith(h -> {
					assertThat(h.getStatus()).isEqualTo(Status.UP);
					assertThat(h.getDetails()).hasSize(50);
				})
				.verifyComplete();

	}

	@Test
	public void timeoutReachedUsesFallback() {
		this.indicator.addHealthIndicator("slow", new TimeoutHealth(10000, Status.UP))
				.addHealthIndicator("fast", new TimeoutHealth(10, Status.UP))
				.timeoutStrategy(100, UNKNOWN_HEALTH);
		StepVerifier.create(this.indicator.health()).consumeNextWith(h -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnlyKeys("slow", "fast");
			assertThat(h.getDetails().get("slow")).isEqualTo(UNKNOWN_HEALTH);
			assertThat(h.getDetails().get("fast")).isEqualTo(HEALTHY);
		}).verifyComplete();
	}

	@Test
	public void timeoutNotReached() {
		this.indicator.addHealthIndicator("slow", new TimeoutHealth(10000, Status.UP))
				.addHealthIndicator("fast", new TimeoutHealth(10, Status.UP))
				.timeoutStrategy(20000, null);
		StepVerifier.withVirtualTime(this.indicator::health)
				.expectSubscription()
				.thenAwait(Duration.ofMillis(10000))
				.consumeNextWith(h -> {
					assertThat(h.getStatus()).isEqualTo(Status.UP);
					assertThat(h.getDetails()).containsOnlyKeys("slow", "fast");
					assertThat(h.getDetails().get("slow")).isEqualTo(HEALTHY);
					assertThat(h.getDetails().get("fast")).isEqualTo(HEALTHY);
				})
				.verifyComplete();
	}

}
