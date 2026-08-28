package com.zippp.rabbitconsumer.handler;

import com.zippp.rabbitconsumer.exception.RabbitConsumerFailedToParseException;
import com.zippp.rabbitconsumer.model.ConsumerParsedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

public class MessageParser {

    private static final Logger log = LoggerFactory.getLogger(MessageParser.class);

    public static final String CORRELATION_HEADER = "x-message-id";

    public static <T> ConsumerParsedMessage<T> parsedMessage(
            Message amqpMessage,
            Class<T> resClass,
            JsonMapper jsonMapper)
    throws RabbitConsumerFailedToParseException {
        String correlationId = extractCorrelationId(amqpMessage);

        T payload;
        try {
            String body = new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
            payload = jsonMapper.readValue(body, resClass);
        } catch (Exception ex) {
            log.error("(RABBIT-CONSUMER) - Failed to deserialize consumed message with corrId={}", correlationId, ex);
            throw new RabbitConsumerFailedToParseException(ex);
        }

        log.debug("(RABBIT-CONSUMER) - Received message with corrId={}, payload={}", correlationId, payload);
        return new ConsumerParsedMessage<>(correlationId, payload);
    }

    private static String extractCorrelationId(Message amqpMessage) {
        Object headerValue = amqpMessage.getMessageProperties().getHeader(CORRELATION_HEADER);
        if (headerValue == null) {
            log.warn("Missing {} header on signup request — generating fallback id",
                    CORRELATION_HEADER);
            return java.util.UUID.randomUUID().toString();
        }
        return headerValue.toString();
    }
}
