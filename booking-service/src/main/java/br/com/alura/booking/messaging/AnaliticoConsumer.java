package br.com.alura.booking.messaging;

import br.com.alura.booking.event.ReservaCriadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AnaliticoConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnaliticoConsumer.class);

    private final Set<String> eventosProcessados =
            Collections.synchronizedSet(new HashSet<>());
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @KafkaListener(
        topics  = "${reserva.kafka.topic}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onReservaRegistrada(ReservaCriadaEvent event) {
        if (eventosProcessados.contains(event.getEventId())) {
            log.warn("[ANALYTICS] Evento duplicado ignorado: {}", event.getEventId());
            return;
        }
        eventosProcessados.add(event.getEventId());
        processedCount.incrementAndGet();
        log.info("[ANALYTICS] ReservaRegistrada: reservaId={}, salaId={}, usuarioId={}",
                event.getReservaId(), event.getSalaId(), event.getUsuarioId());
    }

    public int getProcessedCount() { return processedCount.get(); }

    public void resetCounters() {
        processedCount.set(0);
        eventosProcessados.clear();
    }
}
