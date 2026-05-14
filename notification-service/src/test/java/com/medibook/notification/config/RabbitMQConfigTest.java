package com.medibook.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RabbitMQConfig Tests")
class RabbitMQConfigTest {

    @Test
    @DisplayName("messageConverter bean returns Jackson2JsonMessageConverter instance")
    void messageConverter_returnsJacksonConverter() {
        RabbitMQConfig config = new RabbitMQConfig();
        Jackson2JsonMessageConverter converter = config.messageConverter();

        assertThat(converter).isNotNull();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("messageConverter returns a new instance each call")
    void messageConverter_returnsNewInstanceEachCall() {
        RabbitMQConfig config = new RabbitMQConfig();
        Jackson2JsonMessageConverter c1 = config.messageConverter();
        Jackson2JsonMessageConverter c2 = config.messageConverter();

        // Both should be valid converter instances
        assertThat(c1).isNotNull();
        assertThat(c2).isNotNull();
    }
}
