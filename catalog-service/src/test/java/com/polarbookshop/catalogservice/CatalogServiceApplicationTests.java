package com.polarbookshop.catalogservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.polarbookshop.catalogservice.domain.Book;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Testcontainers
class CatalogServiceApplicationTests {

    @Autowired private WebTestClient webTestClient;

    @Container
    private static final KeycloakContainer keyCloakContainer =
            new KeycloakContainer("quay.io/keycloak/keycloak:22.0.0")
                    .withRealmImportFile("test-realm-config.json");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keyCloakContainer.getAuthServerUrl() + "realms/PolarBookshop");
    }

    private static KeyCloakToken bjornTokens;
    private static KeyCloakToken isabelleTokens;

    @BeforeAll
    static void generateAccessTokens() {
        WebClient webClient =
                WebClient.builder()
                        .baseUrl(
                                keyCloakContainer.getAuthServerUrl()
                                        + "realms/PolarBookshop/protocol/openid-connect/token")
                        .defaultHeader(
                                HttpHeaders.CONTENT_TYPE,
                                MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                        .build();
        isabelleTokens = authenticateWith("isabelle", "password", webClient);
        bjornTokens = authenticateWith("bjorn", "password", webClient);
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

    private record KeyCloakToken(String accessToken) {
        @JsonCreator
        private KeyCloakToken(@JsonProperty("access_token") final String accessToken) {
            this.accessToken = accessToken;
        }
    }

    @Test
    void whenPostRequestWithEmployeeRoleThenBookCreated() {
        Book book = Book.of("1212121212", "Book", "Author of this book", 12.3, "publisher");
        webTestClient
                .post()
                .uri("/books")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleTokens.accessToken()))
                .bodyValue(book)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Book.class)
                .value(
                        createdBook -> {
                            assertThat(createdBook).isNotNull();
                            assertThat(createdBook.isbn()).isEqualTo(book.isbn());
                        });
    }

    @Test
    void whenPostRequestWithCustomerRoleThenREturn403() {
        Book book = Book.of("1212121212", "Book", "Author of this book", 12.3, "publisher");
        webTestClient
                .post()
                .uri("/books")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornTokens.accessToken()))
                .bodyValue(book)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void whenPostRequestUnauthenticatedThenReturn401() {
        Book book = Book.of("1212121212", "Book", "Author of this book", 12.3, "publisher");
        webTestClient
                .post()
                .uri("/books")
                .bodyValue(book)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void whenGetRequestWithEmployeeRoleThenBookReturned() {
        String isbn = "1313231313";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        Book createdBook = addBook(book);

        webTestClient
                .get()
                .uri("/books/" + isbn)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleTokens.accessToken()))
                .exchange()
                .expectBody(Book.class)
                .value(
                        b -> {
                            assertThat(b).isNotNull();
                            assertThat(b.isbn()).isEqualTo(createdBook.isbn());
                        });
    }

    private Book addBook(Book book) {
        return webTestClient
                .post()
                .uri("/books")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleTokens.accessToken()))
                .bodyValue(book)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Book.class)
                .value(b -> assertThat(b).isNotNull())
                .returnResult()
                .getResponseBody();
    }

    @Test
    void whenGetRequestWithCustomerRoleThenBookReturned() {
        String isbn = "1313131313";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        Book createdBook = addBook(book);

        webTestClient
                .get()
                .uri("/books/" + isbn)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornTokens.accessToken()))
                .exchange()
                .expectBody(Book.class)
                .value(
                        b -> {
                            assertThat(b).isNotNull();
                            assertThat(b.isbn()).isEqualTo(createdBook.isbn());
                        });
    }

    @Test
    void whenGetRequestUnauthenticatedThenBookReturned() {
        String isbn = "1313131323";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        Book createdBook = addBook(book);

        webTestClient
                .get()
                .uri("/books/" + isbn)
                .exchange()
                .expectBody(Book.class)
                .value(
                        b -> {
                            assertThat(b).isNotNull();
                            assertThat(b.isbn()).isEqualTo(createdBook.isbn());
                        });
    }

    @Test
    void whenPutRequestWithEmployeeRoleThenBookUpdated() {
        String isbn = "1414141444";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        Book createdBook = addBook(book);

        Book bookToUpdated =
                Book.of(
                        createdBook.isbn(),
                        createdBook.title(),
                        createdBook.author(),
                        100.0,
                        "publisher1");
        webTestClient
                .put()
                .uri("/books/" + bookToUpdated.isbn())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleTokens.accessToken()))
                .bodyValue(bookToUpdated)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Book.class)
                .value(
                        updatedBook -> {
                            assertThat(updatedBook).isNotNull();
                            assertThat(updatedBook.isbn()).isEqualTo(bookToUpdated.isbn());
                            assertThat(updatedBook.title()).isEqualTo(bookToUpdated.title());
                            assertThat(updatedBook.author()).isEqualTo(bookToUpdated.author());
                            assertThat(updatedBook.price()).isEqualTo(bookToUpdated.price());
                            assertThat(updatedBook.publisher())
                                    .isEqualTo(bookToUpdated.publisher());
                        });
    }

    @Test
    void whenPutRequestWithCustomerRoleThenReturn403() {
        String isbn = "1414141454";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        Book createdBook = addBook(book);

        Book bookToUpdated =
                Book.of(
                        createdBook.isbn(),
                        createdBook.title(),
                        createdBook.author(),
                        100.0,
                        "publisher1");
        webTestClient
                .put()
                .uri("/books/" + bookToUpdated.isbn())
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornTokens.accessToken()))
                .bodyValue(bookToUpdated)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void whenPutRequestUnauthenticatedThenReturn401() {
        String isbn = "1414141464";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        Book createdBook = addBook(book);

        Book bookToUpdated =
                Book.of(
                        createdBook.isbn(),
                        createdBook.title(),
                        createdBook.author(),
                        100.0,
                        "publisher1");
        webTestClient
                .put()
                .uri("/books/" + bookToUpdated.isbn())
                .bodyValue(bookToUpdated)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void whenDeleteRequestWithEmployeeRoleThenBookDeleted() {
        String isbn = "1515151545";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        addBook(book);

        webTestClient
                .delete()
                .uri("/books/" + isbn)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(isabelleTokens.accessToken()))
                .exchange()
                .expectStatus()
                .isNoContent();

        webTestClient
                .get()
                .uri("/books/" + isbn)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .value(
                        s ->
                                assertThat(s)
                                        .isEqualTo(
                                                "The book with ISBN " + isbn + " was not found."));
    }

    @Test
    void whenDeleteRequestWithCustomerRoleThenReturn403() {
        String isbn = "1515151555";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        addBook(book);

        webTestClient
                .delete()
                .uri("/books/" + isbn)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(bjornTokens.accessToken()))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void whenDeleteRequestUnauthenticatedThenReturn401() {
        String isbn = "1515151565";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3, "publisher");
        addBook(book);

        webTestClient.delete().uri("/books/" + isbn).exchange().expectStatus().isUnauthorized();
    }
}
