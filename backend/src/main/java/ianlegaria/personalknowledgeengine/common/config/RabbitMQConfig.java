package ianlegaria.personalknowledgeengine.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INGESTION_QUEUE = "document.ingestion";
    public static final String INGESTION_DLQ = "document.ingestion.dlq";
    public static final String EXCHANGE = "knowledge-engine";
    public static final String INGESTION_ROUTING_KEY = "document.ingest";
    public static final String DLQ_ROUTING_KEY = "document.ingest.dlq";

    public static final String FLASHCARD_QUEUE           = "flashcard.generation";
    public static final String FLASHCARD_ROUTING_KEY     = "flashcard.generate";
    public static final String FLASHCARD_DLQ             = "flashcard.generation.dlq";
    public static final String FLASHCARD_DLQ_ROUTING_KEY = "flashcard.generate.dlq";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue ingestionQueue() {
        return QueueBuilder.durable(INGESTION_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(INGESTION_DLQ).build();
    }

    @Bean
    public Binding ingestionBinding(Queue ingestionQueue, DirectExchange exchange) {
        return BindingBuilder.bind(ingestionQueue).to(exchange).with(INGESTION_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue, DirectExchange exchange) {
        return BindingBuilder.bind(deadLetterQueue).to(exchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue flashcardQueue() {
        return QueueBuilder.durable(FLASHCARD_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", FLASHCARD_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue flashcardDlq() {
        return QueueBuilder.durable(FLASHCARD_DLQ).build();
    }

    @Bean
    public Binding flashcardBinding(Queue flashcardQueue, DirectExchange exchange) {
        return BindingBuilder.bind(flashcardQueue).to(exchange).with(FLASHCARD_ROUTING_KEY);
    }

    @Bean
    public Binding flashcardDlqBinding(Queue flashcardDlq, DirectExchange exchange) {
        return BindingBuilder.bind(flashcardDlq).to(exchange).with(FLASHCARD_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}