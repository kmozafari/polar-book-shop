package com.polarbookshop.catalogservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.polarbookshop.catalogservice.domain.Book;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;

@JsonTest
public class BookJsonTests {

    @Autowired private JacksonTester<Book> json;

    @Test
    void testSerialize() throws Exception {
        Instant date = Instant.now();
        Book book =
                new Book(
                        1L,
                        "1234567890",
                        "Book",
                        "Author of this book",
                        12.3,
                        "pub",
                        date,
                        date,
                        "Kourosh",
                        "Ali",
                        3);
        JsonContent<Book> jsonContent = json.write(book);
        assertThat(jsonContent)
                .extractingJsonPathNumberValue("@.id")
                .isEqualTo(book.id().intValue());
        assertThat(jsonContent).extractingJsonPathStringValue("@.isbn").isEqualTo(book.isbn());
        assertThat(jsonContent).extractingJsonPathStringValue("@.title").isEqualTo(book.title());
        assertThat(jsonContent).extractingJsonPathStringValue("@.author").isEqualTo(book.author());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.price").isEqualTo(book.price());
        assertThat(jsonContent)
                .extractingJsonPathStringValue("@.publisher")
                .isEqualTo(book.publisher());
        assertThat(jsonContent)
                .extractingJsonPathStringValue("@.createdDate")
                .isEqualTo(date.toString());
        assertThat(jsonContent)
                .extractingJsonPathStringValue("@.lastModifiedDate")
                .isEqualTo(date.toString());
        assertThat(jsonContent)
                .extractingJsonPathStringValue("@.createdBy")
                .isEqualTo(book.createdBy());
        assertThat(jsonContent)
                .extractingJsonPathStringValue("@.lastModifiedBy")
                .isEqualTo(book.lastModifiedBy());
        assertThat(jsonContent)
                .extractingJsonPathNumberValue("@.version")
                .isEqualTo(book.version());
    }

    @Test
    void testDeserialize() throws IOException {
        Instant date = Instant.parse("2023-10-01T10:57:36.896685028Z");
        String content =
                """
                        {
                            "id": 1,
                            "isbn":"1234567891",
                            "title":"Book 1",
                            "author":"mr. author",
                            "price": 12.34,
                            "publisher": "pub",
                            "createdDate": "2023-10-01T10:57:36.896685028Z",
                            "lastModifiedDate":"2023-10-01T10:57:36.896685028Z",
                            "createdBy":"kourosh",
                            "lastModifiedBy":"ali",
                            "version": 2
                        }
                        """;
        ObjectContent<Book> objectContent = json.parse(content);
        assertThat(objectContent)
                .usingRecursiveComparison()
                .isEqualTo(
                        new Book(
                                1L,
                                "1234567891",
                                "Book 1",
                                "mr. author",
                                12.34,
                                "pub",
                                date,
                                date,
                                "kourosh",
                                "ali",
                                2));
    }
}
