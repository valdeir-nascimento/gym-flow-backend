package br.com.gym.flow.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thin Micrometer wrapper so domain code does not import Micrometer types
 * directly. Counters are cached per tag combination to avoid per-call lookups.
 */
@Component
public class DomainMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public DomainMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void countOrderPlaced(String modality) {
        counter("cardapio.orders.placed", "modality", modality).increment();
    }

    public void countPaymentApproved() {
        counter("cardapio.payments.approved").increment();
    }

    public void countPaymentRejected() {
        counter("cardapio.payments.rejected").increment();
    }

    public void countNotificationDispatched(String channel, String status) {
        counter("cardapio.notifications.dispatched", "channel", channel, "status", status).increment();
    }

    public void countReviewSubmitted() {
        counter("cardapio.reviews.submitted").increment();
    }

    public Timer paymentGatewayLatency() {
        return timer("cardapio.payments.gateway.latency");
    }

    private Counter counter(String name, String... tags) {
        String key = name + "|" + String.join("|", tags);
        return counters.computeIfAbsent(key, k -> Counter.builder(name).tags(tags).register(registry));
    }

    private Timer timer(String name, String... tags) {
        String key = name + "|" + String.join("|", tags);
        return timers.computeIfAbsent(key, k -> Timer.builder(name).tags(tags).register(registry));
    }
}
