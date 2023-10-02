package com.polarbookshop.catalogservice.domain;

import com.polarbookshop.catalogservice.config.DataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@Import(DataConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
public class BookRepositoryJdbcTests {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void findAllBooks() {
        var book1 = Book.of("1234567891", "title1", "author1", 12.0);
        var book2 = Book.of("1234567892", "title2", "author2", 13.0);
        jdbcAggregateTemplate.saveAll(List.of(book1, book2));
        Iterable<Book> actualBooks = bookRepository.findAll();
        assertThat(StreamSupport.stream(actualBooks.spliterator(), true)
                .filter(book -> book.isbn().equals("1234567891") || book.isbn().equals("1234567892"))
                .toList().size()).isEqualTo(2);
    }

    @Test
    void findBookByIsbnWhenExisting() {
        var bookIsbn = "1234567890";
        jdbcAggregateTemplate.insert(Book.of(bookIsbn, "title", "author", 12.4));
        Optional<Book> actualBook = bookRepository.findByIsbn(bookIsbn);
        assertThat(actualBook).isPresent();
        assertThat(actualBook.get().isbn()).isEqualTo(bookIsbn);
    }

    @Test
    void findBookByIsbnWhenNotExisting() {
        jdbcAggregateTemplate.insert(Book.of("1234567890", "title", "author", 12.4));
        Optional<Book> actualBook = bookRepository.findByIsbn("1234567891");
        assertThat(actualBook).isEmpty();
    }

    @Test
    void existsByIsbnWhenExisting() {
        var bookIsbn = "1234567890";
        jdbcAggregateTemplate.insert(Book.of(bookIsbn, "title", "author", 12.4));
        boolean exists = bookRepository.existsByIsbn(bookIsbn);
        assertThat(exists).isTrue();
    }

    @Test
    void existsByIsbnWhenNotExisting() {
        jdbcAggregateTemplate.insert(Book.of("1234567890", "title", "author", 12.4));
        boolean exists = bookRepository.existsByIsbn("1234567891");
        assertThat(exists).isFalse();
    }

    @Test
    void deleteByIsbn() {
        String isbn = "1234567890";
        Book persistedBook = jdbcAggregateTemplate.insert(Book.of("1234567890", "title", "author", 12.4));
        bookRepository.deleteByIsbn(isbn);
        Book book = jdbcAggregateTemplate.findById(persistedBook.id(), Book.class);
        assertThat(book).isNull();
    }
}
