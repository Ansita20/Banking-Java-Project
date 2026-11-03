package bankproject.common.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import bankproject.common.events.BankEvent;
import bankproject.common.events.EventCodec;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxRepository repository;

    /** Must join the caller's transaction; publishing outside one would defeat the outbox. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String key, BankEvent event) {
        repository.save(new OutboxEvent(topic, key, event.getClass().getSimpleName(), EventCodec.encode(event)));
    }
}
