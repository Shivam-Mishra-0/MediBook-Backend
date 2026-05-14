package com.medibook.appointment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RabbitMQConfig Tests")
class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    @DisplayName("Exchange bean is configured correctly")
    void exchangeBeanIsConfiguredCorrectly() {
        TopicExchange exchange = config.exchange();

        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("Queue beans use the expected names")
    void queueBeansUseExpectedNames() {
        Queue booked = config.bookedQueue();
        Queue cancelled = config.cancelledQueue();
        Queue completed = config.completedQueue();

        assertThat(booked.getName()).isEqualTo(RabbitMQConfig.QUEUE_BOOKED);
        assertThat(cancelled.getName()).isEqualTo(RabbitMQConfig.QUEUE_CANCELLED);
        assertThat(completed.getName()).isEqualTo(RabbitMQConfig.QUEUE_COMPLETED);
        assertThat(booked.isDurable()).isTrue();
        assertThat(cancelled.isDurable()).isTrue();
        assertThat(completed.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Bindings use the correct routing keys")
    void bindingsUseCorrectRoutingKeys() {
        TopicExchange exchange = config.exchange();

        Binding bookedBinding = config.bookedBinding(config.bookedQueue(), exchange);
        Binding cancelledBinding = config.cancelledBinding(config.cancelledQueue(), exchange);
        Binding completedBinding = config.completedBinding(config.completedQueue(), exchange);

        assertThat(bookedBinding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE);
        assertThat(bookedBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_BOOKED);
        assertThat(bookedBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.KEY_BOOKED);

        assertThat(cancelledBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_CANCELLED);
        assertThat(cancelledBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.KEY_CANCELLED);

        assertThat(completedBinding.getDestination()).isEqualTo(RabbitMQConfig.QUEUE_COMPLETED);
        assertThat(completedBinding.getRoutingKey()).isEqualTo(RabbitMQConfig.KEY_COMPLETED);
    }

    @Test
    @DisplayName("JSON converter bean uses Jackson")
    void jsonConverterBeanUsesJackson() {
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("RabbitTemplate uses the JSON converter")
    void rabbitTemplateUsesJsonConverter() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        assertThat(rabbitTemplate.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
