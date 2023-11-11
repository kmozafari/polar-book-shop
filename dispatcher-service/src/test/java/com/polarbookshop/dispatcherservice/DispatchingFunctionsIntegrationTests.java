package com.polarbookshop.dispatcherservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Function;

@FunctionalSpringBootTest
public class DispatchingFunctionsIntegrationTests {

    @Autowired
    private FunctionCatalog functionCatalog;

    @Test
    void packOrder() {
        Function<OrderAcceptedMessage, Long> pack = functionCatalog.lookup(Function.class, "pack");

        Assertions.assertEquals(12L, pack.apply(new OrderAcceptedMessage(12L)));
    }

    @Test
    void labelOrder() {
        Function<Flux<Long>, Flux<OrderDispatchedMessage>> label =
                functionCatalog.lookup(Function.class, "label");
        Long orderId = 12L;
        StepVerifier.create(label.apply(Flux.just(orderId)))
                .expectNextMatches(orderDispatchedMessage -> orderDispatchedMessage.orderId().equals(orderId))
                .verifyComplete();
    }

    @Test
    void packAndLabelOrder() {
        Function<OrderAcceptedMessage, Flux<OrderDispatchedMessage>> packAndLabel = functionCatalog.lookup(Function.class, "pack|label");
        Long orderId = 12L;
        StepVerifier.create(packAndLabel.apply(new OrderAcceptedMessage(orderId))).expectNextMatches(orderDispatchedMessage -> orderDispatchedMessage.orderId().equals(orderId)).verifyComplete();
    }
}
