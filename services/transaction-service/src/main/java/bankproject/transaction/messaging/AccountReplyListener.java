package bankproject.transaction.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import bankproject.common.events.AccountReplies.AccountCredited;
import bankproject.common.events.AccountReplies.AccountDebited;
import bankproject.common.events.AccountReplies.AccountRefunded;
import bankproject.common.events.AccountReplies.CreditRejected;
import bankproject.common.events.AccountReplies.DebitRejected;
import bankproject.common.events.BankEvent;
import bankproject.common.events.EventCodec;
import bankproject.common.events.Topics;
import bankproject.transaction.service.TransferSaga;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountReplyListener {

    private final TransferSaga saga;

    @KafkaListener(topics = Topics.ACCOUNT_REPLIES, groupId = "transaction-service")
    public void onReply(String payload) {
        BankEvent event = EventCodec.decode(payload);
        if (event instanceof AccountDebited e) {
            saga.on(e);
        } else if (event instanceof DebitRejected e) {
            saga.on(e);
        } else if (event instanceof AccountCredited e) {
            saga.on(e);
        } else if (event instanceof CreditRejected e) {
            saga.on(e);
        } else if (event instanceof AccountRefunded e) {
            saga.on(e);
        }
    }
}
