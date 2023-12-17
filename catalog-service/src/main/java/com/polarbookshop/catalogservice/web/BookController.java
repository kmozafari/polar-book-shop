package com.polarbookshop.catalogservice.web;

import com.polarbookshop.catalogservice.domain.Book;
import com.polarbookshop.catalogservice.domain.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("books")
@RequiredArgsConstructor
public class BookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    @GetMapping
    public Iterable<Book> get() {
        LOGGER.info("Fetching the list of books in the catalog");
        return bookService.viewBookList();
    }

    @GetMapping("{isbn}")
    public Book getByIsbn(@PathVariable("isbn") String isbn) {
        LOGGER.info("Fetching the book with ISBN {} from the catalog", isbn);
        return bookService.viewBookDetails(isbn);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book add(@Valid @RequestBody Book book) {
        LOGGER.info("Adding a new book to the catalog with ISBN {}", book.isbn());
        return bookService.addBookToCatalog(book);
    }

    @PutMapping("{isbn}")
    public Book edit(@PathVariable String isbn, @Valid @RequestBody Book book) {
        LOGGER.info("Updating book with ISBN {}", isbn);
        return bookService.editBookDetails(isbn, book);
    }

    @DeleteMapping("{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("isbn") String isbn) {
        LOGGER.info("Deleting book with ISBN {}", isbn);
        bookService.removeBookFromCatalog(isbn);
    }
}
