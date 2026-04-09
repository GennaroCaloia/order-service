package com.example.orderservice.metrics;

import com.example.orderservice.domain.enums.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Metriche custom per il dominio degli ordini.
 * Centralizza la registrazione e l'accesso a tutti i meter
 * specifici del servizio — il service layer dipende solo da questa classe.
 */
@Component
@Slf4j
public class OrderMetrics {

    // ── Nomi delle metriche (costanti per evitare typo) ───────
    public static final String ORDERS_CREATED     = "orders.created";
    public static final String ORDERS_STATUS      = "orders.status.transitions";
    public static final String ORDERS_CANCELLED   = "orders.cancelled";
    public static final String PROCESSING_TIME    = "order.processing.time";
    public static final String ACTIVE_ORDERS      = "orders.active";

    private final MeterRegistry registry;

    // Cache dei Counter per status — evita di creare un nuovo
    // meter ad ogni transizione (i meter si registrano una volta sola)
    private final ConcurrentMap<String, Counter> statusCounters =
            new ConcurrentHashMap<>();

    private final Timer processingTimer;

    public OrderMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Timer pre-registrato per il processing degli ordini
        this.processingTimer = Timer.builder(PROCESSING_TIME)
                .description("Tempo di elaborazione per la creazione di un ordine")
                .publishPercentileHistogram()
                .register(registry);
    }

    // ── Increment helpers ─────────────────────────────────────

    /**
     * Incrementa il counter degli ordini creati,
     * taggato per customerId (attenzione alla cardinalità in prod —
     * in un sistema con milioni di customer usa un tag aggregato).
     */
    public void incrementOrdersCreated() {
        Counter.builder(ORDERS_CREATED)
                .description("Numero totale di ordini creati")
                .register(registry)
                .increment();
    }

    /**
     * Traccia ogni transizione di stato con i tag from/to.
     * Permette di costruire in Grafana una matrice delle transizioni.
     */
    public void recordStatusTransition(OrderStatus from, OrderStatus to) {
        String key = from.name() + "_" + to.name();

        statusCounters.computeIfAbsent(key, k ->
                Counter.builder(ORDERS_STATUS)
                        .description("Transizioni di stato degli ordini")
                        .tag("from", from.name())
                        .tag("to",   to.name())
                        .register(registry)
        ).increment();
    }

    /**
     * Traccia le cancellazioni con il motivo (bucketed per evitare
     * alta cardinalità — non usare il reason grezzo come tag).
     */
    public void incrementOrdersCancelled(OrderStatus cancelledFrom) {
        Counter.builder(ORDERS_CANCELLED)
                .description("Ordini cancellati")
                .tag("from_status", cancelledFrom.name())
                .register(registry)
                .increment();
    }

    /**
     * Restituisce il Timer per misurare la durata di operazioni.
     * Usato con try-with-resources nel service:
     *   try (Timer.Sample sample = metrics.startTimer()) { ... }
     */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(processingTimer);
    }
}