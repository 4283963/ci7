package com.aquarium.iot.circuit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class CircuitBreakerRegistry {

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final long DEFAULT_RETRY_MS = 30_000;

    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker get(String name) {
        return breakers.computeIfAbsent(name, k -> new CircuitBreaker(k,
                DEFAULT_FAILURE_THRESHOLD, DEFAULT_RETRY_MS));
    }

    public static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final long retryIntervalMs;

        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>();

        public CircuitBreaker(String name, int failureThreshold, long retryIntervalMs) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.retryIntervalMs = retryIntervalMs;
        }

        public boolean allowRequest() {
            State current = state.get();

            if (current == State.CLOSED) {
                return true;
            }

            if (current == State.OPEN) {
                Instant lastFail = lastFailureTime.get();
                if (lastFail != null &&
                        Instant.now().isAfter(lastFail.plusMillis(retryIntervalMs))) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        log.info("Circuit breaker [{}] transitioning to HALF_OPEN for retry", name);
                        return true;
                    }
                }
                return false;
            }

            return current == State.HALF_OPEN;
        }

        public void recordSuccess() {
            if (state.get() != State.CLOSED) {
                failureCount.set(0);
                state.set(State.CLOSED);
                log.info("Circuit breaker [{}] reset to CLOSED after success", name);
            }
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
            lastFailureTime.set(Instant.now());

            if (state.get() == State.HALF_OPEN) {
                state.set(State.OPEN);
                log.warn("Circuit breaker [{}] OPEN after half-open failure", name);
                return;
            }

            if (failureCount.get() >= failureThreshold && state.get() == State.CLOSED) {
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    log.error("Circuit breaker [{}] OPEN after {} failures", name, failureThreshold);
                }
            }
        }

        public State getState() {
            return state.get();
        }

        public enum State {
            CLOSED, OPEN, HALF_OPEN
        }
    }
}
