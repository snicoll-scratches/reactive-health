package com.example.reactive.health;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.util.Assert;

/**
 *
 * @author Stephane Nicoll
 */
public class ReactiveCompositeHealthIndicator implements ReactiveHealthIndicator {

	private final Map<String, ReactiveHealthIndicator> indicators;

	private final HealthAggregator healthAggregator;

	private Long timeout;

	private Health timeoutHealth;

	private final Function<Mono<Health>, Mono<Health>> timeoutCompose;

	public ReactiveCompositeHealthIndicator(HealthAggregator healthAggregator) {
		this(healthAggregator, new LinkedHashMap<>());
	}

	public ReactiveCompositeHealthIndicator(HealthAggregator healthAggregator,
			Map<String, ReactiveHealthIndicator> indicators) {
		Assert.notNull(healthAggregator, "HealthAggregator must not be null");
		Assert.notNull(indicators, "Indicators must not be null");
		this.indicators = new LinkedHashMap<>(indicators);
		this.healthAggregator = healthAggregator;
		this.timeoutCompose = mono -> this.timeout != null ?
				mono.timeout(Duration.ofMillis(this.timeout), Mono.just(this.timeoutHealth)) :
				mono;
	}

	public ReactiveCompositeHealthIndicator addHealthIndicator(String name,
			ReactiveHealthIndicator indicator) {
		this.indicators.put(name, indicator);
		return this;
	}

	public ReactiveCompositeHealthIndicator timeoutStrategy(long timeout,
			Health timeoutHealth) {
		this.timeout = timeout;
		this.timeoutHealth = (timeoutHealth != null ? timeoutHealth
				: Health.unknown().build());
		return this;
	}

	@Override
	public Mono<Health> health() {
		return Flux.fromIterable(this.indicators.entrySet())
				.flatMap(entry -> Mono.just(entry.getKey())
						.and(entry.getValue().health().compose(this.timeoutCompose)))
				.collectMap(Tuple2::getT1, Tuple2::getT2)
				.map(this.healthAggregator::aggregate);
	}

}
