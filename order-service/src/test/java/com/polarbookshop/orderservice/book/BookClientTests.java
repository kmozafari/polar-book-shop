package com.polarbookshop.orderservice.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@TestMethodOrder(MethodOrderer.Random.class)
public class BookClientTests {

    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    public void setup() throws IOException {
        this.mockWebServer = new MockWebServer();
        mockWebServer.start();
        var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();
        bookClient = new BookClient(webClient);
    }

    @AfterEach
    public void clean() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void whenBookExistsThenReturnBook() {
        String isbn = "1234567890";
        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "isbn": %s,
                            "title":"book1",
                            "author":"author1",
                            "price":10.2,
                            "publisher":"publisher1"
                        }
                        """.formatted(isbn));
        mockWebServer.enqueue(mockResponse);
        Mono<Book> book = bookClient.getBookByIsbn(isbn);
        StepVerifier.create(book)
                .expectNextMatches(b -> isbn.equals(b.isbn()) && "book1".equals(b.title()))
                .verifyComplete();
    }

    @Test
    public void whenBookNotExistsThenReturnEmpty() {
        String isbn = "1234567890";
        var mockResponse = new MockResponse()
                .setStatus(HttpStatus.NOT_FOUND.toString())
                .setBody("Book not found");
        mockWebServer.enqueue(mockResponse);
        var book = bookClient.getBookByIsbn(isbn);
        StepVerifier
                .create(book)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    public void whenTimeoutOccurredReturnEmpty() {
        String isbn = "1234567890";
        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "isbn": %s,
                            "title":"book1",
                            "author":"author1",
                            "price":10.2,
                            "publisher":"publisher1"
                        }
                        """.formatted(isbn))
                .setBodyDelay(4, TimeUnit.SECONDS);
        mockWebServer.enqueue(mockResponse);
        Mono<Book> book = bookClient.getBookByIsbn(isbn);
        StepVerifier.create(book)
                .expectNextCount(0)
                .verifyComplete();
    }
}
