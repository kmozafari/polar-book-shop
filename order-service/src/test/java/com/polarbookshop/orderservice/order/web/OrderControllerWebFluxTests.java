package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.polarbookshop.orderservice.config.SecurityConfig;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderService;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
@Import(SecurityConfig.class)
public class OrderControllerWebFluxTests {

    @Autowired private WebTestClient webClient;

    @MockBean private OrderService orderService;

    @MockBean private ReactiveJwtDecoder jwtDecoder;

    @Test
    public void whenBookNotAvailableAndAuthenticatedThenRejectOrder() {
        OrderRequest orderRequest = new OrderRequest("1234567890", 2);
        Order actualOrder =
                OrderService.buildRejectOrder(orderRequest.isbn(), orderRequest.quantity());
        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
                .willReturn(Mono.just(actualOrder));
        webClient
                .mutateWith(
                        SecurityMockServerConfigurers.mockJwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_employee")))
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Order.class)
                .value(
                        o -> {
                            assertThat(actualOrder).isNotNull();
                            assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                        });
    }

    @Test
    public void whenBookNotAvailableAndUnauthenticatedThenReturn401() {
        OrderRequest orderRequest = new OrderRequest("1234567890", 2);
        Order actualOrder =
                OrderService.buildRejectOrder(orderRequest.isbn(), orderRequest.quantity());
        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
                .willReturn(Mono.just(actualOrder));
        webClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
