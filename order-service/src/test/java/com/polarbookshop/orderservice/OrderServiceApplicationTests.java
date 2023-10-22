package com.polarbookshop.orderservice;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import com.polarbookshop.orderservice.order.web.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceApplicationTests {

    @Container
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private BookClient bookClient;

    @DynamicPropertySource
    public static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgres://%s:%s/%s",
                postgres.getHost(),
                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgres.getDatabaseName());
    }

    @Test
    public void whenGetOrdersThenReturn() {
        String isbn = "2134567890";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        Order expectedOrder = webClient.post()
                .uri("/orders")
                .bodyValue(new OrderRequest(isbn, 1))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();
        assertThat(expectedOrder).isNotNull();

        webClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(orders -> assertThat(orders.stream().filter(o -> o.bookIsbn().equals(isbn)).findAny()).isNotEmpty());
    }

    @Test
    public void whenPostRequestAndBookExistsThenOrderAccepted() {
        String isbn = "2134567890";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        OrderRequest orderRequest = new OrderRequest(isbn, 2);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        webClient.post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .value(order -> {
                    assertThat(order).isNotNull();
                    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
                    assertThat(order.bookIsbn()).isEqualTo(isbn);
                    assertThat(order.bookName()).isEqualTo(book.title());
                    assertThat(order.bookPrice()).isEqualTo(book.price());
                    assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
                });
    }

    @Test
    public void whenPostRequestAndBookNotExistsThenOrderRejected() {
        String isbn = "2134567890";
        OrderRequest orderRequest = new OrderRequest(isbn, 2);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.empty());
        webClient.post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .value(order -> {
                    assertThat(order).isNotNull();
                    assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
                    assertThat(order.bookIsbn()).isEqualTo(isbn);
                    assertThat(order.bookName()).isEqualTo(null);
                    assertThat(order.bookPrice()).isEqualTo(null);
                    assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
                });
    }
}
