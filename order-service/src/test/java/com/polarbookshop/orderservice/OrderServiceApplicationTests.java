package com.polarbookshop.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import com.polarbookshop.orderservice.order.web.OrderRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestChannelBinderConfiguration.class)
class OrderServiceApplicationTests {

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Container
    private static final KeycloakContainer keycloakContainer =
            new KeycloakContainer("quay.io/keycloak/keycloak:22.0.0")
                    .withRealmImportFile("test-realm-config.json");

    @Autowired private WebTestClient webTestClient;

    @MockBean private BookClient bookClient;

    @Autowired private OutputDestination outputDestination;

    @Autowired private ObjectMapper objectMapper;

    private static KeyCloakToken isabelleToken;
    private static KeyCloakToken bjornToken;

    @DynamicPropertySource
    public static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakContainer.getAuthServerUrl() + "realms/PolarBookshop");
    }

    private static String r2dbcUrl() {
        return String.format(
                "r2dbc:postgres://%s:%s/%s",
                postgres.getHost(),
                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgres.getDatabaseName());
    }

    @BeforeAll
    static void generateAccessTokens() {
        WebClient webClient =
                WebClient.builder()
                        .baseUrl(
                                keycloakContainer.getAuthServerUrl()
                                        + "realms/PolarBookshop/protocol/openid-connect/token")
                        .defaultHeader(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .build();
        isabelleToken = authenticateWith("isabelle", "password", webClient);
        bjornToken = authenticateWith("bjorn", "password", webClient);
    }

    private static KeyCloakToken authenticateWith(
            String username, String password, WebClient webClient) {
        return webClient
                .post()
                .body(
                        BodyInserters.fromFormData("grant_type", "password")
                                .with("client_id", "polar-test")
                                .with("username", username)
                                .with("password", password))
                .retrieve()
                .bodyToMono(KeyCloakToken.class)
                .block();
    }

    private record KeyCloakToken(String token) {
        @JsonCreator
        private KeyCloakToken(@JsonProperty("access_token") final String token) {
            this.token = token;
        }
    }

    @Test
    public void whenGetOrdersWithEmployeeRoleThenReturn() throws IOException {
        String isbn = "2134567890";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        Order expectedOrder =
                webTestClient
                        .post()
                        .uri("/orders")
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleToken.token()))
                        .bodyValue(new OrderRequest(isbn, 1))
                        .exchange()
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Order.class)
                        .returnResult()
                        .getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(
                        objectMapper.readValue(
                                outputDestination.receive().getPayload(),
                                OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));
        webTestClient
                .get()
                .uri("/orders")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleToken.token()))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(
                        orders ->
                                assertThat(
                                                orders.stream()
                                                        .filter(o -> o.bookIsbn().equals(isbn))
                                                        .findAny())
                                        .isNotEmpty());
    }

    @Test
    public void whenGetOrdersWithCustomerRoleThenReturn() throws IOException {
        String isbn = "2134567891";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        Order expectedOrder =
                webTestClient
                        .post()
                        .uri("/orders")
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornToken.token()))
                        .bodyValue(new OrderRequest(isbn, 1))
                        .exchange()
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Order.class)
                        .returnResult()
                        .getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(
                        objectMapper.readValue(
                                outputDestination.receive().getPayload(),
                                OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));
        webTestClient
                .get()
                .uri("/orders")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornToken.token()))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(
                        orders ->
                                assertThat(
                                                orders.stream()
                                                        .filter(o -> o.bookIsbn().equals(isbn))
                                                        .findAny())
                                        .isNotEmpty());
    }

    @Test
    public void whenGetOrdersWithAnotherUserThenReturnNothing() throws IOException {
        String isbn = "2134567891";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        Order expectedOrder =
                webTestClient
                        .post()
                        .uri("/orders")
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleToken.token()))
                        .bodyValue(new OrderRequest(isbn, 1))
                        .exchange()
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Order.class)
                        .returnResult()
                        .getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(
                        objectMapper.readValue(
                                outputDestination.receive().getPayload(),
                                OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));
        webTestClient
                .get()
                .uri("/orders")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornToken.token()))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(orders -> assertThat(orders.isEmpty()));
    }

    @Test
    public void whenGetOrdersUnauthenticatedThenReturn401() throws IOException {
        String isbn = "2134567892";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        Order expectedOrder =
                webTestClient
                        .post()
                        .uri("/orders")
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleToken.token()))
                        .bodyValue(new OrderRequest(isbn, 1))
                        .exchange()
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Order.class)
                        .returnResult()
                        .getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(
                        objectMapper.readValue(
                                outputDestination.receive().getPayload(),
                                OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));
        webTestClient.get().uri("/orders").exchange().expectStatus().isUnauthorized();
    }

    @Test
    public void whenPostRequestAndBookExistsAndAuthenticatedThenOrderAccepted() throws IOException {
        String isbn = "2134567893";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        OrderRequest orderRequest = new OrderRequest(isbn, 2);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        Order createdOrder =
                webTestClient
                        .post()
                        .uri("/orders")
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleToken.token()))
                        .bodyValue(orderRequest)
                        .exchange()
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(Order.class)
                        .returnResult()
                        .getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(createdOrder.bookIsbn()).isEqualTo(isbn);
        assertThat(createdOrder.bookName()).isEqualTo(book.title());
        assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());

        assertThat(
                        objectMapper.readValue(
                                outputDestination.receive().getPayload(),
                                OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
    }

    @Test
    public void whenPostRequestAndBookExistsAndUnauthenticatedThenReturn401() {
        String isbn = "2134567893";
        Book book = new Book(isbn, "book name", "author1", 12.5);
        OrderRequest orderRequest = new OrderRequest(isbn, 2);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));
        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    public void whenPostRequestAndBookNotExistsAndAuthenticatedThenOrderRejected() {
        String isbn = "2134567894";
        OrderRequest orderRequest = new OrderRequest(isbn, 2);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.empty());
        webTestClient
                .post()
                .uri("/orders")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornToken.token()))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Order.class)
                .value(
                        order -> {
                            assertThat(order).isNotNull();
                            assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
                            assertThat(order.bookIsbn()).isEqualTo(isbn);
                            assertThat(order.bookName()).isEqualTo(null);
                            assertThat(order.bookPrice()).isEqualTo(null);
                            assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
                        });
    }

    @Test
    public void whenPostRequestAndBookNotExistsAndUnauthenticatedThenReturn401() {
        String isbn = "2134567895";
        OrderRequest orderRequest = new OrderRequest(isbn, 2);
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.empty());
        webTestClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
