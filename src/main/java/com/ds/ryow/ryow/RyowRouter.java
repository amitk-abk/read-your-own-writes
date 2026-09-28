package com.ds.ryow.ryow;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RyowRouter {
	public static final String HEADER_NAME = "X-RYOW-header";

	private final Clock clock;
	private final Duration window;
	private final AtomicInteger replicaCounter = new AtomicInteger();

	public RyowRouter(Clock clock, @Value("${ryow.window:PT1M}") Duration window) {
		this.clock = clock;
		this.window = window;
	}

	public RyowDecision decide(String headerValue) {
		if (headerValue != null) {
			try {
				long writeTime = Long.parseLong(headerValue.trim());
				long age = Instant.now(clock).toEpochMilli() - writeTime;
				if (age >= 0 && age < window.toMillis()) {
					return RyowDecision.primaryDecision();
				}
				if (age < 0) {
					return RyowDecision.primaryDecision();
				}
			} catch (NumberFormatException ignored) {
				// Invalid client tokens cannot establish read-your-own-write affinity.
			}
		}
		return (replicaCounter.getAndIncrement() & 1) == 0
				? RyowDecision.replica("readOnlyFirst")
				: RyowDecision.replica("readOnlySecond");
	}

	public long nowEpochMillis() {
		return Instant.now(clock).toEpochMilli();
	}
}