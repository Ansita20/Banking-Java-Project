package bankproject.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

import bankproject.common.events.Topics;

@Configuration
@EnableScheduling
public class MessagingConfig {

    /** Retry a failing record 3 times, then park it on "<topic>.DLT" instead of blocking the partition. */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    @Bean
    KafkaAdmin.NewTopics bankTopics(@Value("${bank.kafka.partitions:3}") int partitions,
                                    @Value("${bank.kafka.replicas:1}") short replicas) {
        return new KafkaAdmin.NewTopics(
                topic(Topics.IDENTITY_EVENTS, partitions, replicas),
                topic(Topics.ACCOUNT_COMMANDS, partitions, replicas),
                topic(Topics.ACCOUNT_REPLIES, partitions, replicas),
                topic(Topics.ACCOUNT_EVENTS, partitions, replicas),
                topic(Topics.TRANSFER_EVENTS, partitions, replicas));
    }

    private static NewTopic topic(String name, int partitions, short replicas) {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build();
    }
}
