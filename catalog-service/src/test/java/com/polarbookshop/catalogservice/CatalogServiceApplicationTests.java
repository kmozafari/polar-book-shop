package com.polarbookshop.catalogservice;

import com.polarbookshop.catalogservice.domain.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class CatalogServiceApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenPostRequestThenBookCreated() {
        Book book = Book.of("1212121212", "Book", "Author of this book", 12.3);
        webTestClient.post()
                .uri("/books")
                .bodyValue(book)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Book.class).value(createdBook -> {
                    assertThat(createdBook).isNotNull();
                    assertThat(createdBook.isbn()).isEqualTo(book.isbn());
                });
    }

    @Test
    void whenGetRequestWithIdThenBookReturned() {
        String isbn = "1313131313";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3);
        Book createdBook = webTestClient
                .post()
                .uri("/books")
                .bodyValue(book)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Book.class)
                .value(b -> assertThat(b).isNotNull())
                .returnResult().getResponseBody();

        webTestClient
                .get()
                .uri("/books/" + isbn)
                .exchange()
                .expectBody(Book.class)
                .value(b -> {
                    assertThat(b).isNotNull();
                    assertThat(b.isbn()).isEqualTo(createdBook.isbn());
                });
    }

    @Test
    void whenPutRequestThenBookUpdated() {
        String isbn = "1414141414";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3);
        Book createdBook = webTestClient
                .post()
                .uri("/books")
                .bodyValue(book)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Book.class)
                .value(b -> assertThat(b).isNotNull())
                .returnResult().getResponseBody();

        Book bookToUpdated = Book.of(createdBook.isbn(), createdBook.title(), createdBook.author(), 100.0);
        webTestClient
                .put()
                .uri("/books/" + bookToUpdated.isbn())
                .bodyValue(bookToUpdated)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Book.class)
                .value(updatedBook -> {
                    assertThat(updatedBook).isNotNull();
                    assertThat(updatedBook.isbn()).isEqualTo(bookToUpdated.isbn());
                    assertThat(updatedBook.title()).isEqualTo(bookToUpdated.title());
                    assertThat(updatedBook.author()).isEqualTo(bookToUpdated.author());
                    assertThat(updatedBook.price()).isEqualTo(bookToUpdated.price());
                });
    }

    @Test
    void whenDeleteRequestThenBookDeleted() {
        String isbn = "1515151515";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3);
        webTestClient
                .post()
                .uri("/books")
                .bodyValue(book)
                .exchange()
                .expectStatus().isCreated();

        webTestClient
                .delete()
                .uri("/books/" + isbn)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient
                .get()
                .uri("/books/" + isbn)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(String.class)
                .value(s -> {
                    assertThat(s).isEqualTo("The book with ISBN " + isbn + " was not found.");
                });
    }
}