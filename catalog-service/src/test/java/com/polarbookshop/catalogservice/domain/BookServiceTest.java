package com.polarbookshop.catalogservice.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @InjectMocks
    private BookService bookService;

    @Mock
    private BookRepository bookRepository;

    @Test
    void whenBookToCreateAlreadyExistsThenThrows() {
        String isbn = "1234567890";
        Book book = Book.of(isbn, "Book", "Author of this book", 12.3,"publisher");
        when(bookRepository.existsByIsbn(isbn))
                .thenReturn(true);
        Assertions.assertThatThrownBy(() -> bookService.addBookToCatalog(book))
                .isInstanceOf(BookAlreadyExistsException.class)
                .hasMessage("A book with ISBN " + isbn + " already exists.");
    }

    @Test
    void whenBookToReadDoesNotExistsThenThrows() {
        String isbn = "1234567890";
        when(bookRepository.findByIsbn(isbn))
                .thenReturn(Optional.empty());
        Assertions.assertThatThrownBy(() -> bookService.viewBookDetails(isbn))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessage("The book with ISBN " + isbn + " was not found.");
    }
}
