package com.polarbookshop.dispatcherservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.io.IOException;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
public class FunctionStreamIntegrationTests {

    @Autowired
    private InputDestination inputDestination;

    @Autowired
    private OutputDestination outputDestination;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void whenOrderAcceptedThenDispatched() throws IOException {
        Long orderId = 12L;
        Message<OrderAcceptedMessage> message = MessageBuilder.withPayload(new OrderAcceptedMessage(orderId)).build();
        Message<OrderDispatchedMessage> expectedMessage = MessageBuilder.withPayload(new OrderDispatchedMessage(orderId)).build();
        inputDestination.send(message);
        Assertions.assertEquals(
                expectedMessage.getPayload(),
                objectMapper.readValue(outputDestination.receive().getPayload(), OrderDispatchedMessage.class)
        );
    }
}
